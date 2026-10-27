package com.example.Escolar.Service;

import com.example.Escolar.Dto.MatriculaOpcionesResponse;
import com.example.Escolar.Dto.MatriculaOpcionesResponse.GradoOpcion;
import com.example.Escolar.Dto.MatriculaOpcionesResponse.SeccionOpcion;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Alumno;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.Grado;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.Matricula;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AlumnoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import com.example.Escolar.Service.OrdenAcademicoService.Posicion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Reglas de en qué grado y sección puede matricularse un alumno en un año:
 * <ul>
 *   <li>Nuevo: cualquier grado.</li>
 *   <li>Antiguo de un grado que no repite (Inicial, 1ro de Primaria): solo el siguiente, sin esperar el cierre.</li>
 *   <li>Antiguo de un grado que repite: espera el cierre del año anterior; luego el mismo o el siguiente.</li>
 * </ul>
 * Solo se ofrecen secciones con aula y con cupo.
 */
@Service
@RequiredArgsConstructor
public class MatriculaReglasService {

    /** Resultado de aplicar las reglas: permitidas null significa cualquier grado. */
    record Regla(String bloqueo, Set<Posicion> permitidas, Posicion sugerida, String ultimoGrado) {
        boolean permite(Grado grado) {
            return permitidas == null || permitidas.contains(new Posicion(grado.getNivel().getIdNivel(), grado.getOrden()));
        }
    }

    private final MatriculaRepository matriculaRepository;
    private final GradoSeccionRepository gradoSeccionRepository;
    private final AnioEscolarRepository anioEscolarRepository;
    private final AlumnoRepository alumnoRepository;
    private final AccesoRepository accesoRepository;
    private final OrdenAcademicoService ordenAcademicoService;
    private final PagoConfigService pagoConfigService;

    public MatriculaOpcionesResponse opciones(Integer idAnio, Integer idAlumno, Integer idMatriculaExcluir) {
        AnioEscolar anio = anioEscolarRepository.findByIdAnioAndAccesoNot(idAnio, eliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Año escolar no encontrado con id " + idAnio));
        Alumno alumno = idAlumno == null ? null : alumnoRepository.findByIdAlumnoAndAccesoNot(idAlumno, eliminado())
                .orElse(null);
        Regla regla = regla(anio, alumno, idMatriculaExcluir);

        MatriculaOpcionesResponse response = new MatriculaOpcionesResponse();
        response.setBloqueo(regla.bloqueo());
        response.setUltimoGrado(regla.ultimoGrado());
        response.setGrados(regla.bloqueo() != null ? List.of() : grados(anio, regla, idMatriculaExcluir));
        return response;
    }

    /** Comprueba en la transacción de guardado que la sección elegida sigue permitida y con cupo. */
    public void validarSeccion(GradoSeccion gradoSeccion, Alumno alumno, Integer idMatriculaExcluir) {
        Regla regla = regla(gradoSeccion.getAnioEscolar(), alumno, idMatriculaExcluir);
        if (regla.bloqueo() != null) {
            throw new IllegalArgumentException(regla.bloqueo());
        }
        if (!regla.permite(gradoSeccion.getGrado())) {
            throw new IllegalArgumentException("El alumno no puede matricularse en " + nombreGrado(gradoSeccion.getGrado())
                    + ": solo puede ir a su mismo grado o al siguiente");
        }
        if (gradoSeccion.getAula() == null) {
            throw new IllegalArgumentException("La sección " + GradoSeccionService.nombreSeccion(gradoSeccion)
                    + " no tiene aula asignada");
        }
        gradoSeccionRepository.bloquear(gradoSeccion.getIdGradoSeccion());
        if (ocupados(gradoSeccion, idMatriculaExcluir) >= gradoSeccion.getAula().getCapacidad()) {
            throw new IllegalArgumentException("La sección " + GradoSeccionService.nombreSeccion(gradoSeccion)
                    + " ya no tiene cupo");
        }
    }

    Regla regla(AnioEscolar anio, Alumno alumno, Integer idMatriculaExcluir) {
        if (anio.getEstado() == AnioEscolarService.ESTADO_CERRADO) {
            return new Regla("El año " + anio.getAnio() + " está cerrado: no admite matrículas", null, null, null);
        }
        if (alumno == null) {
            return new Regla(null, null, null, null);
        }
        List<Matricula> matriculas = matriculaRepository
                .findByAlumnoApoderadoAlumnoIdAlumnoAndAccesoNot(alumno.getIdAlumno(), eliminado()).stream()
                .filter(m -> !m.getIdMatricula().equals(idMatriculaExcluir))
                .toList();
        String nombre = alumno.getNombre() + " " + alumno.getApellidoPat();
        if (matriculas.stream().anyMatch(m -> m.getGradoSeccion().getAnioEscolar().getIdAnio().equals(anio.getIdAnio()))) {
            return new Regla(nombre + " ya está matriculado en " + anio.getAnio(), null, null, null);
        }
        Optional<Matricula> previa = matriculas.stream()
                .filter(m -> m.getGradoSeccion().getAnioEscolar().getAnio().compareTo(anio.getAnio()) < 0)
                .max(Comparator.comparing(m -> m.getGradoSeccion().getAnioEscolar().getAnio()));
        if (previa.isEmpty()) {
            return new Regla(null, null, null, null);
        }

        Grado grado = previa.get().getGradoSeccion().getGrado();
        AnioEscolar anioPrevio = previa.get().getGradoSeccion().getAnioEscolar();
        String ultimo = nombreGrado(grado) + " " + anioPrevio.getAnio();
        Optional<Posicion> siguiente = ordenAcademicoService.siguiente(grado);

        if (Boolean.FALSE.equals(grado.getRepite())) {
            return siguiente
                    .map(s -> new Regla(null, Set.of(s), s, ultimo))
                    .orElseGet(() -> new Regla(nombre + " ya cursó el último grado", null, null, ultimo));
        }
        if (anioPrevio.getEstado() != AnioEscolarService.ESTADO_CERRADO) {
            return new Regla(nombre + " cursa " + nombreGrado(grado) + " en " + anioPrevio.getAnio()
                    + ": podrá matricularse para " + anio.getAnio() + " cuando se cierre el año " + anioPrevio.getAnio(),
                    null, null, ultimo);
        }
        Posicion mismo = ordenAcademicoService.posicion(grado);
        return siguiente
                .map(s -> new Regla(null, Set.of(mismo, s), s, ultimo))
                .orElseGet(() -> new Regla(null, Set.of(mismo), null, ultimo));
    }

    private List<GradoOpcion> grados(AnioEscolar anio, Regla regla, Integer idMatriculaExcluir) {
        Map<Integer, GradoOpcion> porGrado = new LinkedHashMap<>();
        gradoSeccionRepository.findByAnioEscolarIdAnioAndAccesoNot(anio.getIdAnio(), eliminado()).stream()
                .filter(gs -> gs.getAula() != null)
                .filter(gs -> gs.getGrado().getAcceso().getIdAcceso().equals(AccesoConstants.ACTIVO))
                .filter(gs -> regla.permite(gs.getGrado()))
                .sorted(Comparator.comparing(GradoSeccion::getGrado, OrdenAcademicoService.POR_ORDEN_GRADO)
                        .thenComparing(gs -> gs.getTurno().getIdTurno())
                        .thenComparing(GradoSeccionService::nombreSeccion))
                .forEach(gs -> {
                    long ocupados = ocupados(gs, idMatriculaExcluir);
                    if (ocupados >= gs.getAula().getCapacidad()) {
                        return;
                    }
                    GradoOpcion grado = porGrado.computeIfAbsent(gs.getGrado().getIdGrado(),
                            id -> nuevaOpcion(gs.getGrado(), anio, regla));
                    SeccionOpcion seccion = new SeccionOpcion();
                    seccion.setIdGradoSeccion(gs.getIdGradoSeccion());
                    seccion.setSeccion(gs.getSeccion() != null ? gs.getSeccion().getNombre() : "Única");
                    seccion.setIdTurno(gs.getTurno().getIdTurno());
                    seccion.setTurno(gs.getTurno().getNombre());
                    seccion.setCapacidad(gs.getAula().getCapacidad());
                    seccion.setOcupados(ocupados);
                    grado.getSecciones().add(seccion);
                });
        return new ArrayList<>(porGrado.values());
    }

    private GradoOpcion nuevaOpcion(Grado grado, AnioEscolar anio, Regla regla) {
        GradoOpcion opcion = new GradoOpcion();
        opcion.setIdGrado(grado.getIdGrado());
        opcion.setGrado(grado.getNombre());
        opcion.setIdNivel(grado.getNivel().getIdNivel());
        opcion.setNivel(grado.getNivel().getNombre());
        opcion.setMonto(montoOpcional(grado, anio));
        opcion.setSugerido(regla.sugerida() != null && regla.sugerida().equals(ordenAcademicoService.posicion(grado)));
        opcion.setSecciones(new ArrayList<>());
        return opcion;
    }

    private BigDecimal montoOpcional(Grado grado, AnioEscolar anio) {
        try {
            return pagoConfigService.montoDe(grado.getNivel(), anio);
        } catch (IllegalArgumentException sinMonto) {
            return null;
        }
    }

    private long ocupados(GradoSeccion gs, Integer idMatriculaExcluir) {
        long total = matriculaRepository.countByGradoSeccionIdGradoSeccionAndAccesoNot(gs.getIdGradoSeccion(), eliminado());
        if (idMatriculaExcluir != null && matriculaRepository.findByIdMatriculaAndAccesoNot(idMatriculaExcluir, eliminado())
                .map(m -> Objects.equals(m.getGradoSeccion().getIdGradoSeccion(), gs.getIdGradoSeccion()))
                .orElse(false)) {
            total--;
        }
        return total;
    }

    static String nombreGrado(Grado grado) {
        return grado.getNombre() + " " + grado.getNivel().getNombre();
    }

    private Acceso eliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }
}
