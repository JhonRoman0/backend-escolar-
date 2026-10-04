package com.example.Escolar.Service;

import com.example.Escolar.Dto.GradoSeccionResponse;
import com.example.Escolar.Dto.SeccionRequest;
import com.example.Escolar.Dto.SeccionResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.Grado;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.Seccion;
import com.example.Escolar.Model.Turno;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.GradoRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import com.example.Escolar.Repository.SeccionRepository;
import com.example.Escolar.Repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GradoSeccionService {

    /** Estado 1 del anioEscolar: el que esta corriendo. */
    public static final byte ESTADO_ANIO_VIGENTE = 1;

    public static final String MENSAJE_SECCION_EN_USO =
            "No se puede modificar o eliminar esta sección porque cuenta con alumnos matriculados o cursos asignados";

    private final GradoSeccionRepository gradoSeccionRepository;
    private final GradoRepository gradoRepository;
    private final SeccionRepository seccionRepository;
    private final TurnoRepository turnoRepository;
    private final AnioEscolarRepository anioEscolarRepository;
    private final MatriculaRepository matriculaRepository;
    private final AsignacionRepository asignacionRepository;
    private final AccesoRepository accesoRepository;

    public List<GradoSeccionResponse> getSeccionesByGrado(Integer idGrado) {
        Grado grado = gradoRepository.findByIdGradoAndAccesoNot(idGrado, accesoNoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Grado no encontrado con id " + idGrado));
        return gradoSeccionRepository.findByGradoAndAccesoNot(grado, accesoNoEliminado()).stream()
                .filter(gs -> gs.getSeccion() != null)
                .map(this::toResponse)
                .toList();
    }

    /**
     * Crea una seccion nueva sobre un grado que ya existe. El anio no lo elige
     * quien escribe: si no viene se usa el vigente, y si viene se respeta, para
     * que las cargas de los años cerrados sigan siendo posibles.
     */
    @Transactional
    public GradoSeccionResponse create(SeccionRequest request) {
        Grado grado = gradoRepository.findByIdGradoAndAccesoNot(request.getIdGrado(), accesoNoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Grado no encontrado con id " + request.getIdGrado()));
        Turno turno = turnoRepository.findByIdTurnoAndAccesoNot(request.getIdTurno(), accesoNoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con id " + request.getIdTurno()));
        AnioEscolar anioEscolar = request.getIdAnio() != null
                ? buscarAnio(request.getIdAnio())
                : anioVigente();

        String nombre = normalizar(request.getNombre());
        validarNombreLibre(grado, turno, anioEscolar, nombre, null);

        Seccion seccion = new Seccion();
        seccion.setNombre(nombre);
        seccion.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        seccion = seccionRepository.save(seccion);

        GradoSeccion gs = new GradoSeccion();
        gs.setGrado(grado);
        gs.setSeccion(seccion);
        gs.setTurno(turno);
        gs.setAnioEscolar(anioEscolar);
        gs.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        return toResponse(gradoSeccionRepository.save(gs));
    }

    /**
     * Borra una sola seccion. La regla vive aca y no en la UI: si tiene alumnos
     * o cursos la peticion se rechaza aunque llegue desde donde llegue.
     */
    @Transactional
    public void delete(Integer idGradoSeccion) {
        GradoSeccion gs = gradoSeccionRepository.findByIdGradoSeccionAndAccesoNot(idGradoSeccion, accesoNoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Sección no encontrada con id " + idGradoSeccion));

        if (tieneUso(gs)) {
            throw new IllegalArgumentException(MENSAJE_SECCION_EN_USO);
        }

        List<GradoSeccion> hermanas = gradoSeccionRepository.findByGradoAndAccesoNot(gs.getGrado(), accesoNoEliminado());
        if (hermanas.size() <= 1) {
            throw new IllegalArgumentException(
                    "No se puede eliminar la última sección del grado " + gs.getGrado().getNombre()
                            + ": debe quedar al menos una");
        }

        marcarSeccionEliminada(gs);
    }

    /** True si la seccion tiene matriculas o asignaciones que la sostienen. */
    public boolean tieneUso(GradoSeccion gs) {
        Integer id = gs.getIdGradoSeccion();
        return !matriculaRepository.findByGradoSeccionIdGradoSeccionAndAccesoNot(id, accesoNoEliminado()).isEmpty()
                || asignacionRepository.existsByGradoSeccionIdGradoSeccionAndAccesoNot(id, accesoNoEliminado());
    }

    /** Lanza si la seccion esta en uso, con el mensaje que la UI ya conoce. */
    public void validarSinUso(GradoSeccion gs) {
        if (tieneUso(gs)) {
            throw new IllegalArgumentException(MENSAJE_SECCION_EN_USO);
        }
    }

    /**
     * Marca la fila de grado_seccion y tambien la de seccion. Si solo se
     * borrara la primera, la letra quedaria viva para siempre y se acumularian
     * letras huerfanas en la base.
     */
    @Transactional
    public void marcarSeccionEliminada(GradoSeccion gs) {
        Acceso eliminado = accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
        gs.setAcceso(eliminado);
        if (gs.getSeccion() != null) {
            Seccion seccion = gs.getSeccion();
            seccion.setAcceso(eliminado);
            seccionRepository.save(seccion);
        }
        gradoSeccionRepository.save(gs);
    }

    private void validarNombreLibre(Grado grado, Turno turno, AnioEscolar anioEscolar, String nombre, Integer idExcluir) {
        for (GradoSeccion gs : gradoSeccionRepository.findByGradoAndAccesoNot(grado, accesoNoEliminado())) {
            if (gs.getSeccion() == null) {
                continue;
            }
            if (idExcluir != null && gs.getIdGradoSeccion().equals(idExcluir)) {
                continue;
            }
            boolean mismaCombinacion = gs.getTurno().getIdTurno().equals(turno.getIdTurno())
                    && gs.getAnioEscolar().getIdAnio().equals(anioEscolar.getIdAnio())
                    && normalizar(gs.getSeccion().getNombre()).equalsIgnoreCase(nombre);
            if (mismaCombinacion) {
                throw new IllegalArgumentException(
                        "Ya existe la sección " + nombre + " en el turno " + turno.getNombre()
                                + " para el año " + anioEscolar.getAnio());
            }
        }
    }

    public AnioEscolar anioVigente() {
        return anioEscolarRepository
                .findByEstadoAndAccesoNot(ESTADO_ANIO_VIGENTE, accesoNoEliminado())
                .orElseThrow(() -> new IllegalStateException(
                        "No hay un año escolar vigente. Configúralo antes de crear secciones."));
    }

    public AnioEscolar buscarAnio(Integer idAnio) {
        return anioEscolarRepository.findByIdAnioAndAccesoNot(idAnio, accesoNoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado con id " + idAnio));
    }

    public static String normalizar(String nombre) {
        return nombre == null ? "" : nombre.trim();
    }

    private Acceso accesoNoEliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }

    private GradoSeccionResponse toResponse(GradoSeccion gs) {
        SeccionResponse base = toSeccionResponse(gs);
        GradoSeccionResponse response = new GradoSeccionResponse();
        response.setIdGradoSeccion(base.getIdGradoSeccion());
        response.setIdSeccion(base.getIdSeccion());
        response.setNombre(base.getNombre());
        response.setIdTurno(base.getIdTurno());
        response.setTurno(base.getTurno());
        response.setIdAnio(base.getIdAnio());
        response.setAnio(base.getAnio());
        response.setTieneMatriculas(base.isTieneMatriculas());
        response.setTieneAsignaciones(base.isTieneAsignaciones());
        return response;
    }

    /**
     * Proyeccion compartida: el listado de /secciones y el agregado de /grados
     * describen la misma fila, asi que los contadores de uso se calculan en un
     * solo lado y no pueden quedar desincronizados entre las dos respuestas.
     */
    public SeccionResponse toSeccionResponse(GradoSeccion gs) {
        SeccionResponse response = new SeccionResponse();
        response.setIdGradoSeccion(gs.getIdGradoSeccion());
        if (gs.getSeccion() != null) {
            response.setIdSeccion(gs.getSeccion().getIdSeccion());
            response.setNombre(gs.getSeccion().getNombre());
        }
        if (gs.getTurno() != null) {
            response.setIdTurno(gs.getTurno().getIdTurno());
            response.setTurno(gs.getTurno().getNombre());
        }
        if (gs.getAnioEscolar() != null) {
            response.setIdAnio(gs.getAnioEscolar().getIdAnio());
            response.setAnio(gs.getAnioEscolar().getAnio());
        }
        response.setTieneMatriculas(!matriculaRepository
                .findByGradoSeccionIdGradoSeccionAndAccesoNot(gs.getIdGradoSeccion(), accesoNoEliminado()).isEmpty());
        response.setTieneAsignaciones(asignacionRepository
                .existsByGradoSeccionIdGradoSeccionAndAccesoNot(gs.getIdGradoSeccion(), accesoNoEliminado()));
        return response;
    }
}