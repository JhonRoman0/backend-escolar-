package com.example.Escolar.Service;

import com.example.Escolar.Dto.ConfiguracionAcademicaResponse;
import com.example.Escolar.Dto.EvaluacionRequest;
import com.example.Escolar.Dto.EvaluacionResponse;
import com.example.Escolar.Dto.NotasEvaluacionRequest;
import com.example.Escolar.Dto.PeriodoEvaluacionResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Evaluaciones de un curso por sección y periodo. Un docente solo trabaja con
 * sus asignaciones; el resto, con el permiso, con todas. Solo se crean, editan
 * o califican en un año vigente con el periodo abierto.
 */
@Service
@RequiredArgsConstructor
public class EvaluacionService {

    private final EvaluacionRepository evaluacionRepository;
    private final NotaEvaluacionRepository notaRepository;
    private final NotaRepository notaFinalRepository;
    private final AsignacionRepository asignacionRepository;
    private final CompetenciaRepository competenciaRepository;
    private final MatriculaRepository matriculaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AccesoRepository accesoRepository;
    private final AccesoContextoService accesoContextoService;
    private final ConfiguracionAcademicaService configuracionService;
    private final HorarioClaseRepository horarioRepository;
    private final DiaFeriadoRepository feriadoRepository;
    private final ReconocimientoCalificacionRepository reconocimientos;
    private final BloqueoCalificaciones bloqueo;

    private static final String[] DIAS = {"lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"};

    // ── Consultas ──

    @Transactional(readOnly = true)
    public List<EvaluacionResponse.Curso> cursos(Integer idAnio, Integer idUsuario, List<String> roles) {
        return asignacionesVisibles(idUsuario, roles).stream()
                .filter(a -> a.getAnioEscolar().getIdAnio().equals(idAnio))
                .sorted(Comparator.comparing((Asignacion a) -> orden(a.getGradoSeccion().getGrado().getNivel().getOrden()))
                        .thenComparing(a -> orden(a.getGradoSeccion().getGrado().getOrden()))
                        .thenComparing(a -> MatriculaService.gradoYSeccion(a.getGradoSeccion()))
                        .thenComparing(a -> a.getCurso().getNombre()))
                .map(this::aCurso)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvaluacionResponse.Resumen> listar(Integer idAsignacion, Integer periodo, Integer idUsuario, List<String> roles) {
        Asignacion asignacion = asignacionAccesible(idAsignacion, idUsuario, roles);
        List<Evaluacion> evaluaciones = evaluacionRepository
                .findByAsignacionIdAsignacionAndPeriodoAndAccesoNotOrderByFechaAscIdEvaluacionAsc(
                        idAsignacion, periodo.byteValue(), eliminado());
        List<NotaEvaluacion> notas = notaRepository.findByEvaluacionIdEvaluacionInAndAccesoNot(
                evaluaciones.stream().map(Evaluacion::getIdEvaluacion).toList(), eliminado());
        Set<Integer> actuales = alumnosDe(asignacion).stream().map(Matricula::getIdMatricula).collect(Collectors.toSet());
        Map<Integer, Long> conNota = contarNotasActuales(notas, actuales);
        Set<Integer> conHistoricas = notas.stream().map(n -> n.getEvaluacion().getIdEvaluacion()).collect(Collectors.toSet());
        return evaluaciones.stream().map(e -> aResumen(e, conNota.getOrDefault(e.getIdEvaluacion(), 0L).intValue(), actuales.size(), conHistoricas.contains(e.getIdEvaluacion()))).toList();
    }

    @Transactional(readOnly = true)
    public EvaluacionResponse.Detalle detalle(Integer idEvaluacion, Integer idUsuario, List<String> roles) {
        Evaluacion evaluacion = evaluacionAccesible(idEvaluacion, idUsuario, roles);
        Map<Integer, String> notas = notaRepository.findByEvaluacionIdEvaluacionAndAccesoNot(idEvaluacion, eliminado()).stream()
                .collect(Collectors.toMap(n -> n.getMatricula().getIdMatricula(), NotaEvaluacion::getCalificacion));
        List<Matricula> matriculas = alumnosDe(evaluacion.getAsignacion());
        var movimientos = movimientos(matriculas);
        var historial = historial(matriculas, evaluacion.getAsignacion(), movimientos);
        var completas = notaRepository.findByEvaluacionIdEvaluacion(idEvaluacion);
        Set<Integer> conNotaFinal = conNotaFinal(evaluacion);
        List<EvaluacionResponse.Alumno> alumnos = matriculas.stream()
                .map(m -> {
                    Alumno a = m.getAlumnoApoderado().getAlumno();
                    return new EvaluacionResponse.Alumno(m.getIdMatricula(), a.getCodigo(),
                            a.getApellidoPat() + " " + a.getApellidoMat() + ", " + a.getNombre(),
                            notas.get(m.getIdMatricula()), conNotaFinal.contains(m.getIdMatricula()),
                            historial.contains(m.getIdMatricula()), procedencias(m, movimientos).get(idEvaluacion),
                            notaProtegida(completas, m.getIdMatricula(), movimientos));
                })
                .toList();
        int registradas = (int) alumnos.stream().filter(a -> a.calificacion() != null).count();
        return new EvaluacionResponse.Detalle(aResumen(evaluacion, registradas, alumnos.size()),
                configuracion(evaluacion.getAsignacion()).getEscala(), alumnos, evaluacion.getAsignacion().getIdAsignacion());
    }

    // ── Escritura ──

    @Transactional
    public EvaluacionResponse.Resumen crear(EvaluacionRequest request, Integer idUsuario, List<String> roles) {
        if (request.idAsignacion() == null || request.periodo() == null) {
            throw new IllegalArgumentException("Elige el curso y el periodo");
        }
        Asignacion asignacion = asignacionAccesible(request.idAsignacion(), idUsuario, roles);
        PeriodoEvaluacion periodo = periodoAbierto(asignacion, request.periodo());
        Evaluacion evaluacion = new Evaluacion();
        evaluacion.setAsignacion(asignacion);
        evaluacion.setPeriodo(periodo.getNumero());
        aplicar(evaluacion, request, periodo);
        evaluacion.setUsuarioRegistro(usuario(idUsuario));
        evaluacion.setFechaRegistro(LocalDateTime.now());
        evaluacion.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        return aResumen(evaluacionRepository.save(evaluacion), 0, alumnosDe(asignacion).size());
    }

    @Transactional
    public EvaluacionResponse.Resumen editar(Integer idEvaluacion, EvaluacionRequest request, Integer idUsuario, List<String> roles) {
        Evaluacion evaluacion = evaluacionAccesible(idEvaluacion, idUsuario, roles);
        bloqueo.asignacion(evaluacion.getAsignacion().getIdAsignacion());
        bloqueo.evaluacion(idEvaluacion);
        PeriodoEvaluacion periodo = periodoAbierto(evaluacion.getAsignacion(), (int) evaluacion.getPeriodo());
        if (tieneNotas(evaluacion)) {
            // Con notas, cambiar fecha o competencias movería notas ya puestas a otra competencia.
            Set<Integer> actuales = evaluacion.getCompetencias().stream().map(Competencia::getIdCompetencia)
                    .collect(Collectors.toSet());
            if (!request.fecha().equals(evaluacion.getFecha()) || !actuales.equals(request.competencias())) {
                throw new IllegalArgumentException("La evaluación ya tiene notas: solo se puede cambiar el nombre");
            }
            evaluacion.setNombre(request.nombre().trim());
        } else {
            aplicar(evaluacion, request, periodo);
        }
        evaluacionRepository.save(evaluacion);
        return detalle(idEvaluacion, idUsuario, roles).evaluacion();
    }

    @Transactional
    public EvaluacionResponse.Detalle registrarNotas(Integer idEvaluacion, NotasEvaluacionRequest request,
                                                     Integer idUsuario, List<String> roles) {
        request.notas().stream().map(NotasEvaluacionRequest.Item::idMatricula).distinct().sorted().forEach(bloqueo::matricula);
        Evaluacion evaluacion = evaluacionAccesible(idEvaluacion, idUsuario, roles);
        bloqueo.asignacion(evaluacion.getAsignacion().getIdAsignacion());
        bloqueo.evaluacion(idEvaluacion);
        periodoAbierto(evaluacion.getAsignacion(), (int) evaluacion.getPeriodo());
        EscalaCalificacion escala = configuracion(evaluacion.getAsignacion()).getEscala();
        Map<Integer, Matricula> matriculas = alumnosDe(evaluacion.getAsignacion()).stream()
                .collect(Collectors.toMap(Matricula::getIdMatricula, Function.identity()));
        // Todas, también las borradas: la fila de cada alumno se reutiliza.
        Map<Integer, NotaEvaluacion> existentes = notaRepository.findByEvaluacionIdEvaluacion(idEvaluacion).stream()
                .collect(Collectors.toMap(n -> n.getMatricula().getIdMatricula(), Function.identity()));
        Usuario registrador = usuario(idUsuario);
        Acceso activo = accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow();
        Set<Integer> conNotaFinal = conNotaFinal(evaluacion);

        for (NotasEvaluacionRequest.Item item : request.notas()) {
            Matricula matricula = matriculas.get(item.idMatricula());
            if (matricula == null) {
                throw new IllegalArgumentException("Un alumno ya no está matriculado en esta sección");
            }
            if (conNotaFinal.contains(item.idMatricula())) {
                Alumno a = matricula.getAlumnoApoderado().getAlumno();
                throw new IllegalArgumentException("La nota final de " + a.getNombre() + " " + a.getApellidoPat()
                        + " ya está registrada: su nota en esta evaluación ya no se puede cambiar");
            }
            NotaEvaluacion actual = existentes.get(item.idMatricula());
            if (actual != null && (reconocimientos.existsByNotaDestinoIdNotaEvaluacion(actual.getIdNotaEvaluacion())
                    || reconocimientos.existsByNotaOrigenIdNotaEvaluacionAndVigencia(actual.getIdNotaEvaluacion(), ReconocimientoCalificacion.VIGENTE))) {
                throw new IllegalArgumentException("La nota participa en un reconocimiento activo: anúlalo antes de modificarla");
            }
            if (item.calificacion() == null || item.calificacion().isBlank()) {
                if (actual != null && !AccesoConstants.ELIMINADO.equals(actual.getAcceso().getIdAcceso())) {
                    actual.setAcceso(eliminado());
                    notaRepository.save(actual);
                }
                continue;
            }
            String calificacion = Calificaciones.normalizar(escala, item.calificacion());
            NotaEvaluacion nota = actual != null ? actual : new NotaEvaluacion();
            nota.setEvaluacion(evaluacion);
            nota.setMatricula(matricula);
            nota.setCalificacion(calificacion);
            nota.setUsuarioRegistro(registrador);
            nota.setFechaRegistro(LocalDateTime.now());
            nota.setAcceso(activo);
            notaRepository.save(nota);
        }
        return detalle(idEvaluacion, idUsuario, roles);
    }

    @Transactional
    public void eliminar(Integer idEvaluacion, Integer idUsuario, List<String> roles) {
        Evaluacion evaluacion = evaluacionAccesible(idEvaluacion, idUsuario, roles);
        bloqueo.asignacion(evaluacion.getAsignacion().getIdAsignacion());
        bloqueo.evaluacion(idEvaluacion);
        periodoAbierto(evaluacion.getAsignacion(), (int) evaluacion.getPeriodo());
        if (tieneNotas(evaluacion)) {
            throw new IllegalArgumentException("La evaluación tiene notas registradas: bórralas primero si quieres eliminarla");
        }
        evaluacion.setAcceso(eliminado());
        evaluacionRepository.save(evaluacion);
    }

    /**
     * Registro auxiliar: cada alumno con sus notas en todas las evaluaciones del
     * curso y periodo, y por competencia las que la afectan (en vigesimal, su promedio).
     */
    @Transactional(readOnly = true)
    public EvaluacionResponse.Registro registro(Integer idAsignacion, Integer periodo, Integer idUsuario, List<String> roles) {
        Asignacion asignacion = asignacionAccesible(idAsignacion, idUsuario, roles);
        EscalaCalificacion escala = configuracion(asignacion).getEscala();
        List<Evaluacion> evaluaciones = evaluacionRepository
                .findByAsignacionIdAsignacionAndPeriodoAndAccesoNotOrderByFechaAscIdEvaluacionAsc(
                        idAsignacion, periodo.byteValue(), eliminado());
        List<NotaEvaluacion> notas = notaRepository.findByEvaluacionIdEvaluacionInAndAccesoNot(
                evaluaciones.stream().map(Evaluacion::getIdEvaluacion).toList(), eliminado());
        Map<Integer, Map<Integer, String>> porAlumno = new HashMap<>();
        notas.forEach(n -> porAlumno.computeIfAbsent(n.getMatricula().getIdMatricula(), k -> new HashMap<>())
                .put(n.getEvaluacion().getIdEvaluacion(), n.getCalificacion()));
        List<Competencia> competencias = competenciasDe(asignacion.getCurso());
        List<Matricula> alumnos = alumnosDe(asignacion);
        var movimientos = movimientos(alumnos);
        var historial = historial(alumnos, asignacion, movimientos);

        List<EvaluacionResponse.FilaRegistro> filas = alumnos.stream().map(m -> {
            Alumno a = m.getAlumnoApoderado().getAlumno();
            Map<Integer, String> suyas = porAlumno.getOrDefault(m.getIdMatricula(), Map.of());
            List<EvaluacionResponse.PorCompetencia> resumen = competencias.stream().map(c -> {
                List<String> deLaCompetencia = evaluaciones.stream()
                        .filter(e -> e.getCompetencias().stream().anyMatch(x -> x.getIdCompetencia().equals(c.getIdCompetencia())))
                        .map(e -> suyas.get(e.getIdEvaluacion()))
                        .filter(Objects::nonNull)
                        .toList();
                return new EvaluacionResponse.PorCompetencia(c.getIdCompetencia(), deLaCompetencia,
                        escala == EscalaCalificacion.VIGESIMAL ? Calificaciones.promedio(deLaCompetencia) : null);
            }).toList();
            return new EvaluacionResponse.FilaRegistro(m.getIdMatricula(), a.getCodigo(),
                    a.getApellidoPat() + " " + a.getApellidoMat() + ", " + a.getNombre(), suyas, resumen,
                    historial.contains(m.getIdMatricula()), procedencias(m, movimientos));
        }).toList();

        Map<Integer, Long> conNota = contarNotasActuales(notas, alumnos.stream().map(Matricula::getIdMatricula).collect(Collectors.toSet()));
        Set<Integer> conHistoricas = notas.stream().map(n -> n.getEvaluacion().getIdEvaluacion()).collect(Collectors.toSet());
        return new EvaluacionResponse.Registro(escala,
                evaluaciones.stream().map(e -> aResumen(e, conNota.getOrDefault(e.getIdEvaluacion(), 0L).intValue(), alumnos.size(), conHistoricas.contains(e.getIdEvaluacion()))).toList(),
                competencias.stream().map(EvaluacionService::aCompetencia).toList(),
                filas);
    }

    // ── Reglas ──

    /** Por competencia del curso: cuántos alumnos ya tienen su nota final en el periodo. */
    @Transactional(readOnly = true)
    public List<EvaluacionResponse.AvanceFinal> avance(Integer idAsignacion, Integer periodo, Integer idUsuario, List<String> roles) {
        Asignacion asignacion = asignacionAccesible(idAsignacion, idUsuario, roles);
        Map<Integer, EvaluacionResponse.AvanceFinal> avance = avanceFinales(asignacion, periodo.byteValue());
        return competenciasDe(asignacion.getCurso()).stream().map(c -> avance.get(c.getIdCompetencia())).toList();
    }

    private Map<Integer, EvaluacionResponse.AvanceFinal> avanceFinales(Asignacion asignacion, byte periodo) {
        Set<Integer> alumnos = alumnosDe(asignacion).stream().map(Matricula::getIdMatricula).collect(Collectors.toSet());
        Map<Integer, Set<Integer>> conNota = new HashMap<>();
        notaFinalRepository.findByMatriculaGradoSeccionIdGradoSeccionAndBimestreAndAccesoNot(
                        asignacion.getGradoSeccion().getIdGradoSeccion(), periodo, eliminado())
                .stream()
                .filter(n -> alumnos.contains(n.getMatricula().getIdMatricula()))
                .forEach(n -> conNota.computeIfAbsent(n.getCompetencia().getIdCompetencia(), k -> new HashSet<>())
                        .add(n.getMatricula().getIdMatricula()));
        return competenciasDe(asignacion.getCurso()).stream()
                .collect(Collectors.toMap(Competencia::getIdCompetencia, c -> new EvaluacionResponse.AvanceFinal(
                        c.getIdCompetencia(), conNota.getOrDefault(c.getIdCompetencia(), Set.of()).size(), alumnos.size())));
    }

    /**
     * Alumnos que ya tienen la nota final del periodo en alguna competencia que la
     * evaluación afecta: su nota en ella ya no cambia, para no alterar la guía usada.
     */
    private Set<Integer> conNotaFinal(Evaluacion evaluacion) {
        Set<Integer> competencias = evaluacion.getCompetencias().stream().map(Competencia::getIdCompetencia)
                .collect(Collectors.toSet());
        return notaFinalRepository.findByMatriculaGradoSeccionIdGradoSeccionAndBimestreAndAccesoNot(
                        evaluacion.getAsignacion().getGradoSeccion().getIdGradoSeccion(), evaluacion.getPeriodo(), eliminado())
                .stream()
                .filter(n -> afectaCompetencias(n, competencias))
                .map(n -> n.getMatricula().getIdMatricula())
                .collect(Collectors.toSet());
    }

    static boolean afectaCompetencias(Nota nota, Set<Integer> competencias) {
        return competencias.contains(nota.getCompetencia().getIdCompetencia());
    }

    private boolean tieneNotas(Evaluacion evaluacion) {
        return notaRepository.countByEvaluacionIdEvaluacionAndAccesoNot(evaluacion.getIdEvaluacion(), eliminado()) > 0;
    }

    /** Nombre, fecha de clase dentro del periodo y competencias que el curso evalúa. */
    private void aplicar(Evaluacion evaluacion, EvaluacionRequest request, PeriodoEvaluacion periodo) {
        if (request.fecha().isBefore(periodo.getFechaInicio()) || request.fecha().isAfter(periodo.getFechaFin())) {
            throw new IllegalArgumentException("La fecha debe estar dentro del periodo ("
                    + periodo.getFechaInicio().format(PeriodoEvaluacion.FECHA) + " al "
                    + periodo.getFechaFin().format(PeriodoEvaluacion.FECHA) + ")");
        }
        validarDiaDeClase(evaluacion.getAsignacion(), request.fecha());
        Map<Integer, Competencia> disponibles = competenciasDe(evaluacion.getAsignacion().getCurso()).stream()
                .collect(Collectors.toMap(Competencia::getIdCompetencia, Function.identity()));
        Set<Integer> yaMarcadas = evaluacion.getCompetencias().stream().map(Competencia::getIdCompetencia)
                .collect(Collectors.toSet());
        Map<Integer, EvaluacionResponse.AvanceFinal> avance = avanceFinales(evaluacion.getAsignacion(), periodo.getNumero());
        Set<Competencia> elegidas = new HashSet<>();
        for (Integer id : request.competencias()) {
            Competencia competencia = disponibles.get(id);
            if (competencia == null) {
                throw new IllegalArgumentException("Solo se pueden marcar competencias que evalúa "
                        + evaluacion.getAsignacion().getCurso().getNombre());
            }
            // Una competencia ya cerrada para toda la sección daría una evaluación sin nadie a quien calificar.
            EvaluacionResponse.AvanceFinal estado = avance.get(id);
            if (!yaMarcadas.contains(id) && estado != null && estado.completa()) {
                throw new IllegalArgumentException("Todos los alumnos ya tienen nota final en la competencia "
                        + competencia.getCodigo() + " de este periodo: no se puede marcar");
            }
            elegidas.add(competencia);
        }
        evaluacion.setNombre(request.nombre().trim());
        evaluacion.setFecha(request.fecha());
        evaluacion.setCompetencias(elegidas);
    }

    /**
     * Un día en que el curso se dicta en esa sección y que no es feriado. Sin
     * horario registrado vale cualquier día de lunes a viernes. El horario no
     * cambia con el año vigente, que es cuando se crean evaluaciones.
     */
    private void validarDiaDeClase(Asignacion asignacion, LocalDate fecha) {
        List<Integer> dias = diasClase(asignacion);
        int dia = fecha.getDayOfWeek().getValue();
        if (dias.isEmpty() && dia > 5) {
            throw new IllegalArgumentException("El curso no tiene horario registrado: elige un día de lunes a viernes");
        }
        if (!dias.isEmpty() && !dias.contains(dia)) {
            throw new IllegalArgumentException(asignacion.getCurso().getNombre() + " no tiene clase ese día en "
                    + MatriculaService.gradoYSeccion(asignacion.getGradoSeccion()) + " (se dicta "
                    + textoDias(dias) + ")");
        }
        feriadosDe(asignacion.getAnioEscolar()).stream()
                .filter(f -> f.getFecha().equals(fecha))
                .findFirst()
                .ifPresent(f -> {
                    throw new IllegalArgumentException("El " + fecha.format(PeriodoEvaluacion.FECHA) + " es feriado"
                            + (f.getMotivo() != null && !f.getMotivo().isBlank() ? " (" + f.getMotivo() + ")" : ""));
                });
    }

    private List<Integer> diasClase(Asignacion asignacion) {
        return horarioRepository.findByAsignacionIdAsignacionAndAccesoNot(asignacion.getIdAsignacion(), eliminado()).stream()
                .map(h -> (int) h.getDiaSemana())
                .distinct()
                .sorted()
                .toList();
    }

    private List<DiaFeriado> feriadosDe(AnioEscolar anio) {
        return feriadoRepository.findByAccesoNot(eliminado()).stream()
                .filter(f -> f.getAnioEscolar() == null || f.getAnioEscolar().getIdAnio().equals(anio.getIdAnio()))
                .toList();
    }

    /** "lunes y miércoles" */
    private static String textoDias(List<Integer> dias) {
        List<String> nombres = dias.stream().map(d -> DIAS[d - 1]).toList();
        return nombres.size() == 1 ? nombres.get(0)
                : String.join(", ", nombres.subList(0, nombres.size() - 1)) + " y " + nombres.get(nombres.size() - 1);
    }

    /** El periodo, si el año está vigente y el periodo abierto hoy. */
    private PeriodoEvaluacion periodoAbierto(Asignacion asignacion, Integer numero) {
        AnioEscolar anio = asignacion.getAnioEscolar();
        if (anio.getEstado() != AnioEscolarService.ESTADO_VIGENTE) {
            throw new IllegalArgumentException("Solo se registran evaluaciones en un año vigente");
        }
        Nivel nivel = asignacion.getGradoSeccion().getGrado().getNivel();
        PeriodoEvaluacion periodo = configuracionService.periodos(anio, nivel).stream()
                .filter(p -> p.getNumero() == numero)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "El periodo no existe: configúralo en Configuración académica"));
        String estado = periodo.estadoEn(LocalDate.now());
        if (!PeriodoEvaluacion.ABIERTO.equals(estado)) {
            String nombre = configuracion(asignacion).getTipoPeriodo().nombre(numero);
            throw new IllegalArgumentException("El " + nombre.toLowerCase()
                    + (PeriodoEvaluacion.CERRADO.equals(estado) ? " ya cerró" : " aún no empieza")
                    + ": sus evaluaciones son de solo lectura");
        }
        return periodo;
    }

    /** Las de un área sin subcursos, o las que el subcurso tiene ligadas. Solo activas. */
    private List<Competencia> competenciasDe(Curso curso) {
        List<Competencia> todas = curso.getTipo() == TipoCurso.SUBCURSO
                ? List.copyOf(curso.getCompetencias())
                : competenciaRepository.findByCursoIdCursoAndAccesoNot(curso.getIdCurso(), eliminado());
        return todas.stream()
                .filter(c -> AccesoConstants.ACTIVO.equals(c.getAcceso().getIdAcceso()))
                .sorted(Comparator.comparing(Competencia::getOrden))
                .toList();
    }

    private List<Asignacion> asignacionesVisibles(Integer idUsuario, List<String> roles) {
        Optional<Docente> docente = accesoContextoService.esAdmin(roles)
                ? Optional.empty()
                : accesoContextoService.docenteDeUsuario(idUsuario);
        List<Asignacion> asignaciones = docente
                .map(d -> asignacionRepository.findByDocenteIdDocenteAndAccesoNot(d.getIdDocente(), eliminado()))
                .orElseGet(() -> asignacionRepository.findByAccesoNot(eliminado()));
        return asignaciones.stream()
                .filter(a -> AccesoConstants.ACTIVO.equals(a.getAcceso().getIdAcceso()))
                .toList();
    }

    Asignacion asignacionAccesible(Integer idAsignacion, Integer idUsuario, List<String> roles) {
        return asignacionesVisibles(idUsuario, roles).stream()
                .filter(a -> a.getIdAsignacion().equals(idAsignacion))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado o fuera de tus asignaciones"));
    }

    private Evaluacion evaluacionAccesible(Integer idEvaluacion, Integer idUsuario, List<String> roles) {
        Evaluacion evaluacion = evaluacionRepository.findByIdEvaluacionAndAccesoNot(idEvaluacion, eliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Evaluación no encontrada"));
        asignacionAccesible(evaluacion.getAsignacion().getIdAsignacion(), idUsuario, roles);
        return evaluacion;
    }

    /** Matrículas activas de la sección, por apellidos. */
    private List<Matricula> alumnosDe(Asignacion asignacion) {
        return matriculaRepository.findByGradoSeccionIdGradoSeccionAndAccesoNot(
                        asignacion.getGradoSeccion().getIdGradoSeccion(), eliminado()).stream()
                .filter(m -> AccesoConstants.ACTIVO.equals(m.getAcceso().getIdAcceso()))
                .sorted(MatriculaService.ORDEN_REPORTE)
                .toList();
    }

    private ConfiguracionAcademicaResponse configuracion(Asignacion asignacion) {
        return configuracionService.obtener(asignacion.getAnioEscolar().getIdAnio(),
                asignacion.getGradoSeccion().getGrado().getNivel().getIdNivel());
    }

    private EvaluacionResponse.Curso aCurso(Asignacion a) {
        GradoSeccion gs = a.getGradoSeccion();
        ConfiguracionAcademicaResponse config = configuracion(a);
        Curso curso = a.getCurso();
        return new EvaluacionResponse.Curso(a.getIdAsignacion(), curso.getNombre(),
                curso.getCursoPadre() != null ? curso.getCursoPadre().getNombre() : null,
                gs.getGrado().getNivel().getNombre(), gs.getGrado().getNombre(),
                gs.getSeccion() != null ? gs.getSeccion().getNombre() : null, gs.getTurno().getNombre(),
                a.getDocente().getUsuario().getNombre() + " " + a.getDocente().getUsuario().getApellidoPat(),
                a.getAnioEscolar().getIdAnio(), a.getAnioEscolar().getEstado() == AnioEscolarService.ESTADO_VIGENTE,
                config.getEscala(), config.getTipoPeriodo(), config.getPeriodos(),
                competenciasDe(curso).stream().map(EvaluacionService::aCompetencia).toList(),
                diasClase(a),
                feriadosDe(a.getAnioEscolar()).stream().map(DiaFeriado::getFecha).sorted().toList());
    }

    private Map<Integer, Long> contarNotasActuales(List<NotaEvaluacion> notas, Set<Integer> actuales) {
        return notas.stream().filter(n -> actuales.contains(n.getMatricula().getIdMatricula()))
            .collect(Collectors.groupingBy(n -> n.getEvaluacion().getIdEvaluacion(), Collectors.counting()));
    }

    private EvaluacionResponse.Resumen aResumen(Evaluacion e, int registradas, int alumnos) {
        return aResumen(e, registradas, alumnos, e.getIdEvaluacion() != null && tieneNotas(e));
    }

    private EvaluacionResponse.Resumen aResumen(Evaluacion e, int registradas, int alumnos, boolean tieneNotasRegistradas) {
        Asignacion a = e.getAsignacion();
        boolean editable = a.getAnioEscolar().getEstado() == AnioEscolarService.ESTADO_VIGENTE
                && configuracion(a).getPeriodos().stream()
                .filter(p -> p.numero() == e.getPeriodo())
                .map(PeriodoEvaluacionResponse::estado)
                .anyMatch(PeriodoEvaluacion.ABIERTO::equals);
        return new EvaluacionResponse.Resumen(e.getIdEvaluacion(), e.getNombre(), e.getFecha(), e.getPeriodo(),
                e.getCompetencias().stream().sorted(Comparator.comparing(Competencia::getOrden))
                        .map(EvaluacionService::aCompetencia).toList(),
                registradas, alumnos,
                e.getUsuarioRegistro().getNombre() + " " + e.getUsuarioRegistro().getApellidoPat(), editable,
                e.getIdEvaluacion() == null ? 0 : conNotaFinal(e).size(), tieneNotasRegistradas);
    }

    private static EvaluacionResponse.Competencia aCompetencia(Competencia c) {
        return new EvaluacionResponse.Competencia(c.getIdCompetencia(), c.getCodigo(), c.getNombre());
    }

    private static int orden(Integer valor) {
        return valor == null ? Integer.MAX_VALUE : valor;
    }

    private Usuario usuario(Integer idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("No se pudo identificar al usuario"));
    }

    private List<ReconocimientoCalificacionRepository.Movimiento> movimientos(List<Matricula> alumnos) {
        return alumnos.isEmpty() ? List.of() : reconocimientos.movimientosDe(alumnos.stream().map(Matricula::getIdMatricula).toList());
    }
    private Set<Integer> historial(List<Matricula> alumnos, Asignacion a, List<ReconocimientoCalificacionRepository.Movimiento> movimientos) {
        if (alumnos.isEmpty()) return Set.of();
        Set<Integer> ids = new HashSet<>(notaRepository.alumnosConHistorial(alumnos.stream().map(Matricula::getIdMatricula).toList(), a.getCurso().getIdCurso(), a.getAnioEscolar().getIdAnio(), a.getGradoSeccion().getIdGradoSeccion(), eliminado()));
        movimientos.stream().filter(r -> java.util.Objects.equals(r.getCurso(), a.getCurso().getIdCurso()) && java.util.Objects.equals(r.getAnio(), a.getAnioEscolar().getIdAnio())).forEach(r -> ids.add(r.getMatricula()));
        return ids;
    }
    private boolean notaProtegida(List<NotaEvaluacion> notas, Integer matricula, List<ReconocimientoCalificacionRepository.Movimiento> movimientos) {
        return notas.stream().filter(n -> n.getMatricula().getIdMatricula().equals(matricula)).anyMatch(n -> movimientos.stream().anyMatch(r -> java.util.Objects.equals(r.getDestino(), n.getIdNotaEvaluacion()) || (Byte.valueOf(ReconocimientoCalificacion.VIGENTE).equals(r.getVigencia()) && java.util.Objects.equals(r.getOrigen(), n.getIdNotaEvaluacion()))));
    }
    private Map<Integer, com.example.Escolar.Dto.ReconocimientoCalificacionResponse.Procedencia> procedencias(Matricula m, List<ReconocimientoCalificacionRepository.Movimiento> movimientos) {
        return movimientos.stream().filter(r -> r.getMatricula().equals(m.getIdMatricula()) && Byte.valueOf(ReconocimientoCalificacion.VIGENTE).equals(r.getVigencia())).collect(Collectors.toMap(r -> r.getEvaluacion(), r -> new com.example.Escolar.Dto.ReconocimientoCalificacionResponse.Procedencia(r.getId(), r.getGrado() + (r.getSeccion() == null ? "" : " " + r.getSeccion()), r.getNombre())));
    }

    private Acceso eliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }
}
