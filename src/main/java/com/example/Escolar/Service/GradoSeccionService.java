package com.example.Escolar.Service;

import com.example.Escolar.Dto.GradoSeccionResponse;
import com.example.Escolar.Dto.SeccionActualizarRequest;
import com.example.Escolar.Dto.SeccionRequest;
import com.example.Escolar.Dto.SeccionResponse;
import com.example.Escolar.Dto.SeccionesBatchRequest;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GradoSeccionService {

    /** Estado 1 del anioEscolar: el que esta corriendo. */
    public static final byte ESTADO_ANIO_VIGENTE = 1;

    /** Estado 2 del anioEscolar: terminal, no admite secciones nuevas. */
    public static final byte ESTADO_ANIO_CERRADO = 2;

    /** Estado 3 del anioEscolar: creado pero aun no iniciado. */
    public static final byte ESTADO_ANIO_POR_COMENZAR = 3;

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
     * quien escribe: si no viene se resuelve con {@link #anioActivo()} (por
     * comenzar habilitado o vigente), y si viene se respeta, siempre que el ano
     * admita secciones nuevas (vigente o por comenzar ya iniciado).
     *
     * <p>Es el caso de una sola letra del lote de {@link #crearLote}, y comparte
     * con el la resolucion de grado, turno y anio y el guardado, para que los dos
     * caminos no puedan terminar validando distinto.
     */
    @Transactional
    public GradoSeccionResponse create(SeccionRequest request) {
        Combo combo = combo(request.getIdGrado(), request.getIdTurno(), request.getIdAnio());
        String nombre = normalizar(request.getNombre());
        validarNombreLibre(combo, nombre, null);
        return guardar(combo, nombre);
    }

    /**
     * Crea varias secciones que comparten grado, turno y anio.
     *
     * <p>El lote se valida entero antes de guardar la primera letra. Esa es toda
     * la diferencia con encadenar N llamadas a {@link #create}: sin la validacion
     * previa, una peticion de A, B, A dejaria A y B ya escritas cuando la
     * tercera rebotara, y el usuario tendria que limpiar a mano.
     */
    @Transactional
    public List<GradoSeccionResponse> crearLote(SeccionesBatchRequest request) {
        Combo combo = combo(request.getIdGrado(), request.getIdTurno(), request.getIdAnio());

        List<String> nombres = new ArrayList<>();
        Set<String> pendientes = new HashSet<>();
        for (String bruto : request.getNombres()) {
            String nombre = normalizar(bruto);
            if (!pendientes.add(nombre.toLowerCase())) {
                throw new IllegalArgumentException("La sección " + nombre + " está repetida en el envío");
            }
            nombres.add(nombre);
        }

        for (String nombre : nombres) {
            validarNombreLibre(combo, nombre, null);
        }

        return nombres.stream().map(nombre -> guardar(combo, nombre)).toList();
    }

    /**
     * Borra una sola seccion. La regla vive aca y no en la UI: si tiene alumnos
     * o cursos la peticion se rechaza aunque llegue desde donde llegue.
     */
    @Transactional
    public void delete(Integer idGradoSeccion) {
        GradoSeccion gs = gradoSeccionRepository.findByIdGradoSeccionAndAccesoNot(idGradoSeccion, accesoNoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Sección no encontrada con id " + idGradoSeccion));
        if (!habilitadoParaSecciones(gs.getAnioEscolar())) {
            throw new IllegalArgumentException("El año " + gs.getAnioEscolar().getAnio() + " no está habilitado para administrar secciones");
        }
        if (tieneUso(gs)) {
            throw new IllegalArgumentException(MENSAJE_SECCION_EN_USO);
        }
        marcarSeccionEliminada(gs);
    }

    @Transactional
    public GradoSeccionResponse update(Integer idGradoSeccion, SeccionActualizarRequest request) {
        GradoSeccion gs = gradoSeccionRepository.findByIdGradoSeccionAndAccesoNot(idGradoSeccion, accesoNoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Sección no encontrada con id " + idGradoSeccion));

        String nombre = normalizar(request.getNombre());
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la sección es obligatorio");
        }
        if (nombre.length() != 1 || !nombre.matches("^[A-Z]$")) {
            throw new IllegalArgumentException("La sección debe ser una sola letra (A-Z)");
        }

        Combo combo = new Combo(gs.getGrado(), gs.getTurno(), gs.getAnioEscolar());
        validarNombreLibre(combo, nombre, idGradoSeccion);

        if (!habilitadoParaSecciones(gs.getAnioEscolar())) {
            throw new IllegalArgumentException("El año " + gs.getAnioEscolar().getAnio() + " no está habilitado para administrar secciones");
        }

        Seccion s = gs.getSeccion();
        if (s != null) {
            s.setNombre(nombre);
            seccionRepository.save(s);
        } else {
            s = new Seccion();
            s.setNombre(nombre);
            s.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
            seccionRepository.save(s);
            gs.setSeccion(s);
        }
        gradoSeccionRepository.save(gs);
        return toResponse(gs);
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
    public void deleteLote(java.util.List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar al menos una sección para eliminar");
        }

        java.util.LinkedHashSet<Integer> unicos = new java.util.LinkedHashSet<>(ids);
        if (unicos.size() != ids.size()) {
            throw new IllegalArgumentException("La lista de secciones contiene duplicados");
        }

        java.util.List<GradoSeccion> cargadas = new java.util.ArrayList<>();
        java.util.List<com.example.Escolar.Dto.ItemDeleteError> errores = new java.util.ArrayList<>();

        for (Integer id : unicos) {
            java.util.Optional<GradoSeccion> opt = gradoSeccionRepository.findByIdGradoSeccionAndAccesoNot(id, accesoNoEliminado());
            if (opt.isEmpty()) {
                errores.add(new com.example.Escolar.Dto.ItemDeleteError(id, null, "No encontrada o ya fue eliminada"));
                continue;
            }
            cargadas.add(opt.get());
        }

        for (GradoSeccion gs : cargadas) {
            String nombre = (gs.getSeccion() != null) ? gs.getSeccion().getNombre() : null;
            if (!habilitadoParaSecciones(gs.getAnioEscolar())) {
                errores.add(new com.example.Escolar.Dto.ItemDeleteError(
                        gs.getIdGradoSeccion(),
                        nombre,
                        "El año " + gs.getAnioEscolar().getAnio() + " no está habilitado para administrar secciones"
                ));
            }
        }

        for (GradoSeccion gs : cargadas) {
            String nombre = (gs.getSeccion() != null) ? gs.getSeccion().getNombre() : null;
            boolean yaConError = errores.stream()
                    .anyMatch(e -> e.getIdGradoSeccion() != null && e.getIdGradoSeccion().equals(gs.getIdGradoSeccion()));
            if (yaConError) {
                continue;
            }
            if (tieneUso(gs)) {
                errores.add(new com.example.Escolar.Dto.ItemDeleteError(
                        gs.getIdGradoSeccion(),
                        nombre,
                        MENSAJE_SECCION_EN_USO
                ));
            }
        }

        if (!errores.isEmpty()) {
            throw new com.example.Escolar.Exception.BatchDeleteNotAllowedException(
                    "Algunas secciones no pueden eliminarse",
                    errores
            );
        }

        for (GradoSeccion gs : cargadas) {
            marcarSeccionEliminada(gs);
        }
    }

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

    /**
     * Grado, turno y anio de una alta, resueltos y validados. Los tres seSacan
     * siempre de los mismos lugares porque el filtro de duplicados compara
     * justamente contra esa combinacion.
     */
    private Combo combo(Integer idGrado, Integer idTurno, Integer idAnio) {
        Grado grado = gradoRepository.findByIdGradoAndAccesoNot(idGrado, accesoNoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Grado no encontrado con id " + idGrado));
        Turno turno = turnoRepository.findByIdTurnoAndAccesoNot(idTurno, accesoNoEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con id " + idTurno));
        AnioEscolar anioEscolar = idAnio != null ? anioValidoParaSecciones(idAnio) : anioActivo();
        return new Combo(grado, turno, anioEscolar);
    }

    /** Escribe una seccion y su fila de grado_seccion para la combinacion dada. */
    private GradoSeccionResponse guardar(Combo combo, String nombre) {
        Seccion seccion = new Seccion();
        seccion.setNombre(nombre);
        seccion.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        seccion = seccionRepository.save(seccion);

        GradoSeccion gs = new GradoSeccion();
        gs.setGrado(combo.grado());
        gs.setSeccion(seccion);
        gs.setTurno(combo.turno());
        gs.setAnioEscolar(combo.anioEscolar());
        gs.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        return toResponse(gradoSeccionRepository.save(gs));
    }

    /**
     * La seccion no puede existir dos veces con el mismo grado, turno y anio. La
     * comparacion es sin distincion de mayusculas porque "a" y "A" son la misma
     * letra para quien la ve en un listado.
     */
    private void validarNombreLibre(Combo combo, String nombre, Integer idExcluir) {
        for (GradoSeccion gs : gradoSeccionRepository.findByGradoAndAccesoNot(combo.grado(), accesoNoEliminado())) {
            if (gs.getSeccion() == null) {
                continue;
            }
            if (idExcluir != null && gs.getIdGradoSeccion().equals(idExcluir)) {
                continue;
            }
            boolean mismaCombinacion = gs.getTurno().getIdTurno().equals(combo.turno().getIdTurno())
                    && gs.getAnioEscolar().getIdAnio().equals(combo.anioEscolar().getIdAnio())
                    && normalizar(gs.getSeccion().getNombre()).equalsIgnoreCase(nombre);
            if (mismaCombinacion) {
                throw new IllegalArgumentException(
                        "Ya existe la sección " + nombre + " en el turno " + combo.turno().getNombre()
                                + " para el año " + combo.anioEscolar().getAnio());
            }
        }
    }

    /** Triple que define a que combinacion pertenece una seccion nueva. */
    private record Combo(Grado grado, Turno turno, AnioEscolar anioEscolar) {
    }

    public AnioEscolar anioVigente() {
        return anioEscolarRepository
                .findByEstadoAndAccesoNot(ESTADO_ANIO_VIGENTE, accesoNoEliminado())
                .orElseThrow(() -> new IllegalStateException(
                        "No hay un año escolar vigente. Configúralo antes de crear secciones."));
    }

    /**
     * Anio sobre el que se crea una seccion cuando la peticion no trae uno.
     * Prefiere el por comenzar habilitado (el de anio mas alto, que es el
     * proximo ciclo para el que se estan preparando las secciones) y, si no lo
     * hay, el vigente. Solo un anio sin habilitar (cerrado, por comenzar futu-
     * ro o inexistente) bloquea el alta.
     */
    public AnioEscolar anioActivo() {
        Acceso noEliminado = accesoNoEliminado();
        Optional<AnioEscolar> porComenzar = anioEscolarRepository
                .findByEstadoInAndAccesoNot(List.of(ESTADO_ANIO_POR_COMENZAR), noEliminado).stream()
                .filter(this::habilitadoParaSecciones)
                .max(Comparator.comparing(AnioEscolar::getAnio));
        return porComenzar
                .or(() -> anioEscolarRepository.findByEstadoAndAccesoNot(ESTADO_ANIO_VIGENTE, noEliminado))
                .orElseThrow(() -> new IllegalStateException(
                        "No hay un año escolar habilitado (vigente o por comenzar ya iniciado). "
                                + "Configúralo antes de crear secciones."));
    }

    /**
     * El anio elegido a mano aplica la misma regla que el resuelto: vigente o
     * por comenzar cuya fecha de inicio ya llego. El cerrado y el por comenzar
     * sin fecha o todavia futuro se rechazan con un mensaje que dice cual es la
     * fecha limite, porque el caso tipico es preparar el proximo ciclo.
     */
    private AnioEscolar anioValidoParaSecciones(Integer idAnio) {
        AnioEscolar anio = buscarAnio(idAnio);
        if (anio.getEstado() == ESTADO_ANIO_CERRADO) {
            throw new IllegalStateException(
                    "El año " + anio.getAnio() + " está cerrado y no admite secciones nuevas.");
        }
        if (anio.getEstado() == ESTADO_ANIO_POR_COMENZAR) {
            if (anio.getFechaInicio() == null) {
                throw new IllegalStateException(
                        "El año " + anio.getAnio() + " está por comenzar pero no tiene fecha de inicio: "
                                + "no admite secciones todavía.");
            }
            if (anio.getFechaInicio().isAfter(LocalDate.now())) {
                throw new IllegalStateException(
                        "El año " + anio.getAnio() + " aún no inicia: las secciones se habilitan desde el "
                                + enTexto(anio.getFechaInicio()) + ".");
            }
        }
        return anio;
    }

    /**
     * Un por comenzar solo habilita cuando ya arranco su ventana: antes de la
     * fecha de inicio las secciones nuevas serian del ciclo que sigue y
     * entrarian en un anio que nadie esta usando. Sin fecha no hay ventana que
     * esperar, asi que cuenta como todavia no habilitado.
     */
    private boolean habilitadoParaSecciones(AnioEscolar anio) {
        if (anio.getEstado() == ESTADO_ANIO_VIGENTE) {
            return true;
        }
        return anio.getEstado() == ESTADO_ANIO_POR_COMENZAR
                && anio.getFechaInicio() != null
                && !anio.getFechaInicio().isAfter(LocalDate.now());
    }

    /** 2026-10-14 -> 14/10/2026, el formato que la UI muestra al usuario. */
    private String enTexto(LocalDate fecha) {
        return String.format("%02d/%02d/%d", fecha.getDayOfMonth(), fecha.getMonthValue(), fecha.getYear());
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