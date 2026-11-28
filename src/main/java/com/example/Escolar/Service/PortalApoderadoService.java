package com.example.Escolar.Service;

import com.example.Escolar.Dto.HorarioPlanoResponse;
import com.example.Escolar.Dto.PeriodoEvaluacionResponse;
import com.example.Escolar.Dto.RecreoHorarioResponse;
import com.example.Escolar.Dto.PortalApoderadoDtos.*;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.*;
import com.example.Escolar.Service.AccesoContextoService.Alcance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Portal del apoderado: todo de solo lectura y únicamente de sus hijos. Cada
 * consulta recibe la matrícula (alumno + año) y se rechaza si no es de un hijo.
 * Las notas se ven en vivo: apenas el docente las guarda.
 */
@Service
@RequiredArgsConstructor
public class PortalApoderadoService {

    private static final int ULTIMAS_NOTAS = 5;

    private final AccesoContextoService accesoContextoService;
    private final AlumnoApoderadoRepository alumnoApoderadoRepository;
    private final MatriculaRepository matriculaRepository;
    private final AccesoRepository accesoRepository;
    private final AsistenciaService asistenciaService;
    private final HorarioService horarioService;
    private final MatriculaService matriculaService;
    private final MallaEvaluable mallaEvaluable;
    private final ConfiguracionAcademicaService configuracionService;
    private final EvaluacionRepository evaluacionRepository;
    private final NotaEvaluacionRepository notaEvaluacionRepository;
    private final PagoMatriculaRepository pagoMatriculaRepository;
    private final ReconocimientoCalificacionRepository reconocimientoRepository;

    // ============================= HIJOS =============================

    @Transactional(readOnly = true)
    public List<Hijo> hijos(Integer idUsuario, Integer idRolActivo) {
        if (!vinculoApoderadoActivo(idUsuario, idRolActivo)) {
            return List.of();
        }
        Apoderado apoderado = accesoContextoService.apoderadoDeUsuario(idUsuario).orElse(null);
        if (apoderado == null) {
            return List.of();
        }
        Map<Integer, Alumno> alumnos = new LinkedHashMap<>();
        alumnoApoderadoRepository.findByApoderado(apoderado)
                .forEach(v -> alumnos.putIfAbsent(v.getAlumno().getIdAlumno(), v.getAlumno()));
        return alumnos.values().stream()
                .filter(a -> !AccesoConstants.ELIMINADO.equals(a.getAcceso().getIdAcceso()))
                .map(a -> new Hijo(a.getIdAlumno(), nombreCompleto(a), a.getUrlFoto(), matriculasDe(a)))
                .filter(h -> !h.matriculas().isEmpty())
                .sorted(Comparator.comparing(Hijo::nombre))
                .toList();
    }

    /** Del año más reciente al más antiguo. */
    private List<MatriculaHijo> matriculasDe(Alumno alumno) {
        return matriculaRepository.findByAlumnoApoderadoAlumnoIdAlumnoAndAccesoNot(alumno.getIdAlumno(), eliminado()).stream()
                .sorted(Comparator.comparing((Matricula m) -> m.getGradoSeccion().getAnioEscolar().getAnio()).reversed())
                .map(m -> {
                    GradoSeccion gs = m.getGradoSeccion();
                    AnioEscolar anio = gs.getAnioEscolar();
                    return new MatriculaHijo(m.getIdMatricula(), anio.getIdAnio(), anio.getAnio(),
                            anio.getEstado() == AnioEscolarService.ESTADO_VIGENTE,
                            gs.getGrado().getNivel().getNombre(), gs.getGrado().getNombre(),
                            gs.getSeccion() != null ? gs.getSeccion().getNombre() : null,
                            gs.getTurno() != null ? gs.getTurno().getNombre() : null);
                })
                .toList();
    }

    // ============================= RESUMEN =============================

    @Transactional(readOnly = true)
    public Resumen resumen(Integer idMatricula, Integer idUsuario, Integer idRolActivo) {
        Matricula m = matriculaDeHijo(idMatricula, idUsuario, idRolActivo);
        AsistenciaMes asistencia = asistenciaMes(m, mesDeReferencia(m));
        List<UltimaNota> ultimas = notaEvaluacionRepository.findByMatriculaIdMatriculaAndAccesoNot(idMatricula, eliminado()).stream()
                .filter(n -> esActiva(n.getEvaluacion().getAcceso()))
                .sorted(Comparator.comparing(NotaEvaluacion::getFechaRegistro, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(ULTIMAS_NOTAS)
                .map(n -> new UltimaNota(n.getEvaluacion().getAsignacion().getCurso().getNombre(), n.getEvaluacion().getNombre(),
                        n.getEvaluacion().getFecha(), n.getCalificacion(), n.getFechaRegistro()))
                .toList();
        int hoy = LocalDate.now().getDayOfWeek().getValue();
        List<HorarioPlanoResponse> clasesDeHoy = horario(m).stream()
                .filter(h -> h.getDiaSemana() != null && h.getDiaSemana() == hoy)
                .sorted(Comparator.comparing(HorarioPlanoResponse::getHoraInicio))
                .toList();
        return new Resumen(asistencia, ultimas, clasesDeHoy);
    }

    /** Este mes si cae dentro del año escolar; si no, el último mes del año (o el primero si aún no empieza). */
    static YearMonth mesDeReferencia(Matricula m) {
        AnioEscolar anio = m.getGradoSeccion().getAnioEscolar();
        LocalDate hoy = LocalDate.now();
        if (anio.getFechaFin() != null && hoy.isAfter(anio.getFechaFin())) {
            return YearMonth.from(anio.getFechaFin());
        }
        if (anio.getFechaInicio() != null && hoy.isBefore(anio.getFechaInicio())) {
            return YearMonth.from(anio.getFechaInicio());
        }
        return YearMonth.from(hoy);
    }

    // ============================= ASISTENCIA =============================

    @Transactional(readOnly = true)
    public AsistenciaMes asistencia(Integer idMatricula, YearMonth mes, Integer idUsuario, Integer idRolActivo) {
        Matricula m = matriculaDeHijo(idMatricula, idUsuario, idRolActivo);
        return asistenciaMes(m, mes != null ? mes : mesDeReferencia(m));
    }

    private AsistenciaMes asistenciaMes(Matricula m, YearMonth mes) {
        List<DiaAsistencia> dias = asistenciaService.diasDeMatricula(m, mes.atDay(1), mes.atEndOfMonth());
        return new AsistenciaMes(mes.getYear(), mes.getMonthValue(),
                contar(dias, AsistenciaService.ESTADO_PUNTUAL),
                contar(dias, AsistenciaService.ESTADO_TARDANZA),
                contar(dias, AsistenciaService.ESTADO_JUSTIFICADA),
                contar(dias, AsistenciaService.ESTADO_INASISTENCIA),
                dias);
    }

    private static long contar(List<DiaAsistencia> dias, String estado) {
        return dias.stream().filter(d -> estado.equals(d.estado())).count();
    }

    // ============================= NOTAS =============================

    /**
     * Por curso de la malla: cada periodo con sus evaluaciones y la nota del hijo
     * (null si aún no la tiene). Si cambió de sección, también las notas que tuvo
     * en la anterior, salvo las que ya se usaron en la actual ("no rinde"), que
     * aparecen en su evaluación nueva. Solo evaluaciones; lo anulado no se muestra.
     */
    @Transactional(readOnly = true)
    public List<CursoNotas> notas(Integer idMatricula, Integer idUsuario, Integer idRolActivo) {
        Matricula m = matriculaDeHijo(idMatricula, idUsuario, idRolActivo);
        GradoSeccion gs = m.getGradoSeccion();
        AnioEscolar anio = gs.getAnioEscolar();
        Nivel nivel = gs.getGrado().getNivel();
        List<PeriodoEvaluacionResponse> periodos = configuracionService.obtener(anio.getIdAnio(), nivel.getIdNivel()).getPeriodos();

        Map<Integer, List<Evaluacion>> evaluacionesPorCurso = evaluacionRepository
                .findByAsignacionGradoSeccionIdGradoSeccionAndAccesoNot(gs.getIdGradoSeccion(), eliminado()).stream()
                .collect(Collectors.groupingBy(e -> e.getAsignacion().getCurso().getIdCurso()));
        List<NotaEvaluacion> notasDelHijo = notaEvaluacionRepository.findByMatriculaIdMatriculaAndAccesoNot(idMatricula, eliminado()).stream()
                .filter(n -> esActiva(n.getEvaluacion().getAcceso()))
                .toList();
        Map<Integer, String> notaPorEvaluacion = new HashMap<>();
        notasDelHijo.forEach(n -> notaPorEvaluacion.put(n.getEvaluacion().getIdEvaluacion(), n.getCalificacion()));
        Set<Integer> usadasEnLaActual = reconocimientoRepository.findByNotaOrigenMatriculaIdMatricula(idMatricula).stream()
                .filter(r -> Byte.valueOf(ReconocimientoCalificacion.VIGENTE).equals(r.getVigencia()))
                .map(r -> r.getNotaOrigen().getIdNotaEvaluacion())
                .collect(Collectors.toSet());
        // Notas de secciones anteriores, por curso.
        Map<Integer, List<NotaEvaluacion>> deOtraSeccion = notasDelHijo.stream()
                .filter(n -> !gs.getIdGradoSeccion().equals(n.getEvaluacion().getAsignacion().getGradoSeccion().getIdGradoSeccion()))
                .filter(n -> !usadasEnLaActual.contains(n.getIdNotaEvaluacion()))
                .collect(Collectors.groupingBy(n -> n.getEvaluacion().getAsignacion().getCurso().getIdCurso()));

        List<CursoNotas> cursos = new ArrayList<>();
        for (MallaEvaluable.Area area : mallaEvaluable.areas(anio, nivel)) {
            List<Evaluacion> evaluaciones = evaluacionesPorCurso.getOrDefault(area.curso().getIdCurso(), List.of());
            List<NotaEvaluacion> anteriores = deOtraSeccion.getOrDefault(area.curso().getIdCurso(), List.of());
            List<PeriodoNotas> porPeriodo = periodos.stream().map(p -> new PeriodoNotas(p.numero(), p.nombre(), p.estado(),
                    Stream.concat(
                                    anteriores.stream()
                                            .filter(n -> n.getEvaluacion().getPeriodo() == p.numero())
                                            .map(n -> new Fila(n.getEvaluacion(), n.getCalificacion(),
                                                    seccionDe(n.getEvaluacion().getAsignacion().getGradoSeccion()))),
                                    evaluaciones.stream()
                                            .filter(e -> e.getPeriodo() == p.numero())
                                            .map(e -> new Fila(e, notaPorEvaluacion.get(e.getIdEvaluacion()), null)))
                            .sorted(Fila.POR_FECHA)
                            .map(Fila::aNota)
                            .toList()))
                    .toList();
            cursos.add(new CursoNotas(area.curso().getNombre(), porPeriodo));
        }
        return cursos;
    }

    // ============================= HORARIO =============================

    @Transactional(readOnly = true)
    public List<HorarioPlanoResponse> horario(Integer idMatricula, Integer idUsuario, Integer idRolActivo) {
        return horario(matriculaDeHijo(idMatricula, idUsuario, idRolActivo));
    }

    /** Recreos fijos del grado y turno de la sección del hijo. */
    @Transactional(readOnly = true)
    public List<RecreoHorarioResponse> recreos(Integer idMatricula, Integer idUsuario, Integer idRolActivo) {
        return horarioService.recreosDeSeccion(matriculaDeHijo(idMatricula, idUsuario, idRolActivo).getGradoSeccion().getIdGradoSeccion());
    }

    private List<HorarioPlanoResponse> horario(Matricula m) {
        GradoSeccion gs = m.getGradoSeccion();
        return horarioService.planoDeSeccion(gs.getAnioEscolar().getIdAnio(), gs.getIdGradoSeccion());
    }

    // ============================= PAGOS =============================

    @Transactional(readOnly = true)
    public Pagos pagos(Integer idMatricula, Integer idUsuario, Integer idRolActivo) {
        Matricula m = matriculaDeHijo(idMatricula, idUsuario, idRolActivo);
        BigDecimal pagado = matriculaService.pagadoNeto(idMatricula);
        List<Pago> pagos = pagoMatriculaRepository.findByMatriculaIdMatriculaAndAccesoNotOrderByFecha(idMatricula, eliminado()).stream()
                .map(p -> new Pago(p.getFecha(), p.getTipo().name(), p.getMonto(),
                        p.getMetodoPago() != null ? p.getMetodoPago().getNombre() : null, p.getUrlComprobante()))
                .toList();
        BigDecimal monto = m.getMonto();
        return new Pagos(monto, pagado, monto != null ? monto.subtract(pagado) : null, pagos);
    }

    // ============================= ACCESO =============================

    /** La matrícula, solo si es de un hijo del apoderado; si no, como si no existiera. */
    Matricula matriculaDeHijo(Integer idMatricula, Integer idUsuario, Integer idRolActivo) {
        if (!vinculoApoderadoActivo(idUsuario, idRolActivo)) {
            throw new ResourceNotFoundException("Matrícula no encontrada");
        }
        Matricula m = matriculaRepository.findById(idMatricula)
                .filter(x -> !AccesoConstants.ELIMINADO.equals(x.getAcceso().getIdAcceso()))
                .orElseThrow(() -> new ResourceNotFoundException("Matrícula no encontrada"));
        if (!accesoContextoService.esApoderadoDe(idUsuario, m)) {
            throw new ResourceNotFoundException("Matrícula no encontrada");
        }
        return m;
    }

    private boolean vinculoApoderadoActivo(Integer idUsuario, Integer idRolActivo) {
        Alcance vinculo = accesoContextoService.vinculoEfectivo(idUsuario, idRolActivo);
        return vinculo == Alcance.TODO || (vinculo != null && vinculo.incluyeApoderado());
    }

    /** Una evaluación con la nota del hijo, de su sección actual o de una anterior. */
    private record Fila(Evaluacion evaluacion, String calificacion, String otraSeccion) {
        static final Comparator<Fila> POR_FECHA = Comparator
                .comparing((Fila f) -> f.evaluacion().getFecha(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(f -> f.evaluacion().getIdEvaluacion());

        NotaDeEvaluacion aNota() {
            return new NotaDeEvaluacion(evaluacion.getNombre(), evaluacion.getFecha(), calificacion, otraSeccion);
        }
    }

    /** "1ro A · Mañana". */
    private static String seccionDe(GradoSeccion gs) {
        String nombre = gs.getGrado().getNombre() + (gs.getSeccion() != null ? " " + gs.getSeccion().getNombre() : "");
        return gs.getTurno() != null ? nombre + " · " + gs.getTurno().getNombre() : nombre;
    }

    private static boolean esActiva(Acceso acceso) {
        return acceso == null || !AccesoConstants.ELIMINADO.equals(acceso.getIdAcceso());
    }

    private static String nombreCompleto(Alumno a) {
        return String.join(" ", Objects.toString(a.getNombre(), ""), Objects.toString(a.getApellidoPat(), ""),
                Objects.toString(a.getApellidoMat(), "")).trim().replaceAll("\\s+", " ");
    }

    private Acceso eliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }
}
