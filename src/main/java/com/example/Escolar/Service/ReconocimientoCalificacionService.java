package com.example.Escolar.Service;

import com.example.Escolar.Dto.EvaluacionRequest;
import com.example.Escolar.Dto.EvaluacionResponse;
import com.example.Escolar.Dto.HistorialCalificacionesResponse;
import com.example.Escolar.Dto.ReconocimientoCalificacionRequest;
import com.example.Escolar.Dto.ReconocimientoCalificacionResponse;
import com.example.Escolar.Dto.TrasladoResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Notas de alumnos trasladados de sección. La nota de la sección anterior no se
 * mueve: se usa en una evaluación de la sección actual cuando el alumno no la
 * rinde. La original queda intacta y cada uso guarda autor, fecha y, si se
 * anula, el motivo.
 */
@Service
@RequiredArgsConstructor
public class ReconocimientoCalificacionService {
    private final ReconocimientoCalificacionRepository reconocimientos;
    private final NotaEvaluacionRepository notas;
    private final NotaRepository finales;
    private final EvaluacionRepository evaluaciones;
    private final MatriculaRepository matriculas;
    private final UsuarioRepository usuarios;
    private final AccesoRepository accesos;
    private final EvaluacionService evaluacionService;
    private final ConfiguracionAcademicaService configuracion;
    private final AccesoContextoService contexto;
    private final BloqueoCalificaciones bloqueo;

    /** Periodos del nivel y notas finales del alumno: se leen una vez por alumno. */
    private record LecturaAcademica(List<PeriodoEvaluacion> periodos, List<Nota> finales) {
    }

    // ── Consultas ──

    /** Notas del alumno en el curso y año puestas en otras secciones. Solo consulta. */
    @Transactional(readOnly = true)
    public List<HistorialCalificacionesResponse> historial(Integer idMatricula, Integer idAsignacion,
                                                           Integer usuario, Integer idRolActivo) {
        Asignacion actual = evaluacionService.asignacionAccesible(idAsignacion, usuario, idRolActivo);
        Matricula m = matriculas.findById(idMatricula)
                .orElseThrow(() -> new ResourceNotFoundException("Matrícula no encontrada"));
        validarMatricula(m, actual);
        List<ReconocimientoCalificacion> movimientos = reconocimientos.findByNotaOrigenMatriculaIdMatricula(idMatricula);
        // Las copias activas ya se ven como nota de su evaluación de destino.
        Set<Integer> copias = movimientos.stream().filter(ReconocimientoCalificacionService::vigente)
                .map(r -> r.getNotaDestino().getIdNotaEvaluacion()).collect(Collectors.toSet());
        LecturaAcademica lectura = lectura(m, actual);
        return notas.findByMatriculaIdMatriculaAndAccesoNot(idMatricula, eliminado()).stream()
                .filter(n -> !copias.contains(n.getIdNotaEvaluacion()))
                .filter(n -> mismoCursoYAnio(n, actual))
                .filter(n -> !enSeccion(n, actual.getGradoSeccion()) || esOrigen(n, movimientos))
                .sorted(Comparator.comparing((NotaEvaluacion n) -> n.getEvaluacion().getFecha()).reversed())
                .map(n -> aHistorial(n, actual, movimientos, usuario, idRolActivo, lectura))
                .toList();
    }

    /**
     * Alumnos de la sección con notas de otra sección que pueden usar en una
     * evaluación del periodo con esas competencias. Con idEvaluacion, solo los
     * que aún no tienen nota en ella.
     */
    @Transactional(readOnly = true)
    public List<TrasladoResponse> traslados(Integer idAsignacion, Integer periodo, Set<Integer> competencias,
                                            Integer idEvaluacion, Integer usuario, Integer idRolActivo) {
        Asignacion destino = evaluacionService.asignacionAccesible(idAsignacion, usuario, idRolActivo);
        if (periodo == null || competencias == null || competencias.isEmpty()) {
            return List.of();
        }
        Set<Integer> conNota = idEvaluacion == null ? Set.of() : alumnosConNota(idEvaluacion, destino);
        List<Matricula> alumnos = matriculas.findByGradoSeccionIdGradoSeccionAndAccesoNot(
                        destino.getGradoSeccion().getIdGradoSeccion(), eliminado()).stream()
                .filter(m -> esActivo(m.getAcceso()) && !conNota.contains(m.getIdMatricula()))
                .toList();
        if (alumnos.isEmpty()) {
            return List.of();
        }
        Set<Integer> conHistorial = new HashSet<>(notas.alumnosConHistorial(
                alumnos.stream().map(Matricula::getIdMatricula).toList(), destino.getCurso().getIdCurso(),
                destino.getAnioEscolar().getIdAnio(), destino.getGradoSeccion().getIdGradoSeccion(), eliminado()));
        byte numero = periodo.byteValue();
        return alumnos.stream()
                .filter(m -> conHistorial.contains(m.getIdMatricula()))
                .sorted(MatriculaService.ORDEN_REPORTE)
                .map(m -> {
                    LecturaAcademica lectura = lectura(m, destino);
                    List<TrasladoResponse.Opcion> opciones = notas.findByMatriculaIdMatriculaAndAccesoNot(
                                    m.getIdMatricula(), eliminado()).stream()
                            .filter(n -> impedimento(n, destino, numero, competencias, lectura) == null)
                            .sorted(Comparator.comparing((NotaEvaluacion n) -> n.getEvaluacion().getFecha()))
                            .map(ReconocimientoCalificacionService::aOpcion)
                            .toList();
                    Alumno a = m.getAlumnoApoderado().getAlumno();
                    return opciones.isEmpty() ? null : new TrasladoResponse(m.getIdMatricula(), a.getCodigo(),
                            a.getApellidoPat() + " " + a.getApellidoMat() + ", " + a.getNombre(), opciones);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    // ── Escritura ──

    /** Crea la evaluación y, en la misma transacción, registra a los que no la rinden. */
    @Transactional
    public EvaluacionResponse.Resumen crear(EvaluacionRequest request, Integer usuario, Integer idRolActivo) {
        return conNoRinden(evaluacionService.crear(request, usuario, idRolActivo), request, usuario, idRolActivo);
    }

    @Transactional
    public EvaluacionResponse.Resumen editar(Integer idEvaluacion, EvaluacionRequest request,
                                             Integer usuario, Integer idRolActivo) {
        return conNoRinden(evaluacionService.editar(idEvaluacion, request, usuario, idRolActivo), request, usuario, idRolActivo);
    }

    /** Usa una nota de otra sección en una evaluación que ya existe. */
    @Transactional
    public ReconocimientoCalificacionResponse reconocer(ReconocimientoCalificacionRequest request,
                                                        Integer usuario, Integer idRolActivo) {
        NotaEvaluacion origen = nota(request.idNotaOrigen());
        bloqueo.matricula(origen.getMatricula().getIdMatricula());
        Evaluacion destino = evaluacion(request.idEvaluacionDestino());
        ReconocimientoCalificacion r = usar(origen, destino, request.calificacionOrigen(), usuario, idRolActivo);
        return respuesta(r, destino.getAsignacion(), usuario, idRolActivo,
                lectura(origen.getMatricula(), destino.getAsignacion()));
    }

    /** El alumno pasa a rendir la evaluación: se quita la nota usada y queda la auditoría. */
    @Transactional
    public void anular(Integer id, String motivo, Integer usuario, Integer idRolActivo) {
        ReconocimientoCalificacion r = reconocimientos.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reconocimiento no encontrado"));
        bloqueo.matricula(r.getNotaOrigen().getMatricula().getIdMatricula());
        bloqueo.reconocimiento(id);
        Evaluacion e = r.getNotaDestino().getEvaluacion();
        bloqueo.asignacion(e.getAsignacion().getIdAsignacion());
        bloqueo.evaluacion(e.getIdEvaluacion());
        evaluacionService.asignacionAccesible(e.getAsignacion().getIdAsignacion(), usuario, idRolActivo);
        Matricula m = r.getNotaDestino().getMatricula();
        validarMatricula(m, e.getAsignacion());
        if (!vigente(r)) {
            throw new IllegalArgumentException("El reconocimiento ya está anulado");
        }
        if (motivo == null || motivo.isBlank() || motivo.trim().length() > ReconocimientoCalificacion.MOTIVO_MAXIMO) {
            throw new IllegalArgumentException("Escribe un motivo de hasta 500 caracteres");
        }
        String impedimento = bloqueoPeriodoFinal(e.getAsignacion(), e.getPeriodo(), ids(e), lectura(m, e.getAsignacion()));
        if (impedimento != null) {
            throw new IllegalArgumentException(impedimento);
        }
        r.getNotaDestino().setAcceso(eliminado());
        notas.save(r.getNotaDestino());
        r.setVigencia(null);
        r.setUsuarioAnulacion(usuario(usuario));
        r.setFechaAnulacion(LocalDateTime.now());
        r.setMotivoAnulacion(motivo.trim());
        reconocimientos.saveAndFlush(r);
    }

    private EvaluacionResponse.Resumen conNoRinden(EvaluacionResponse.Resumen evaluacion, EvaluacionRequest request,
                                                   Integer usuario, Integer idRolActivo) {
        List<EvaluacionRequest.NoRinde> items = request.noRinden() == null ? List.of() : request.noRinden();
        if (items.isEmpty()) {
            return evaluacion;
        }
        // Al editar basta ACTUALIZAR, pero poner notas pide CREAR.
        if (!contexto.tieneAccion("EVALUACIONES", "CREAR", usuario, idRolActivo)) {
            throw new IllegalArgumentException("No tienes permiso para registrar notas");
        }
        if (items.stream().map(EvaluacionRequest.NoRinde::idMatricula).distinct().count() < items.size()) {
            throw new IllegalArgumentException("Un alumno aparece más de una vez");
        }
        // Lo pendiente (competencias de la evaluación) va a la base antes de bloquear y releer.
        evaluaciones.flush();
        items.stream().map(EvaluacionRequest.NoRinde::idMatricula).sorted().forEach(bloqueo::matricula);
        Evaluacion destino = evaluacion(evaluacion.idEvaluacion());
        for (EvaluacionRequest.NoRinde item : items) {
            NotaEvaluacion origen = nota(item.idNotaOrigen());
            if (!origen.getMatricula().getIdMatricula().equals(item.idMatricula())) {
                throw new IllegalArgumentException("La nota elegida no es de ese alumno");
            }
            usar(origen, destino, null, usuario, idRolActivo);
        }
        return evaluacionService.detalle(evaluacion.idEvaluacion(), usuario, idRolActivo).evaluacion();
    }

    /** calificacionEsperada: si llega, la original debe seguir valiendo eso. */
    private ReconocimientoCalificacion usar(NotaEvaluacion origen, Evaluacion destino, String calificacionEsperada,
                                            Integer usuario, Integer idRolActivo) {
        bloqueo.nota(origen.getIdNotaEvaluacion());
        Stream.of(origen.getEvaluacion().getAsignacion().getIdAsignacion(), destino.getAsignacion().getIdAsignacion())
                .distinct().sorted().forEach(bloqueo::asignacion);
        Stream.of(origen.getEvaluacion().getIdEvaluacion(), destino.getIdEvaluacion())
                .distinct().sorted().forEach(bloqueo::evaluacion);
        evaluacionService.asignacionAccesible(destino.getAsignacion().getIdAsignacion(), usuario, idRolActivo);
        validarMatricula(origen.getMatricula(), destino.getAsignacion());
        validarCurso(origen, destino.getAsignacion());
        String impedimento = impedimento(origen, destino);
        if (impedimento != null) {
            throw new IllegalArgumentException(impedimento);
        }
        if (calificacionEsperada != null && !origen.getCalificacion().equals(calificacionEsperada)) {
            throw new IllegalArgumentException("La nota original cambió: vuelve a abrir la evaluación");
        }
        // La fila del alumno en la evaluación se reutiliza, también si estaba borrada.
        NotaEvaluacion nota = notas.findByEvaluacionIdEvaluacion(destino.getIdEvaluacion()).stream()
                .filter(n -> n.getMatricula().getIdMatricula().equals(origen.getMatricula().getIdMatricula()))
                .findFirst().orElseGet(NotaEvaluacion::new);
        nota.setMatricula(origen.getMatricula());
        nota.setEvaluacion(destino);
        nota.setCalificacion(Calificaciones.normalizar(escala(destino), origen.getCalificacion()));
        nota.setUsuarioRegistro(usuario(usuario));
        nota.setFechaRegistro(LocalDateTime.now());
        nota.setAcceso(activo());
        nota = notas.saveAndFlush(nota);
        ReconocimientoCalificacion r = new ReconocimientoCalificacion();
        r.setNotaOrigen(origen);
        r.setNotaDestino(nota);
        r.setSeccionDestino(seccion(destino));
        r.setCalificacion(nota.getCalificacion());
        r.setEscala(escala(destino));
        r.setUsuarioRegistro(nota.getUsuarioRegistro());
        r.setFechaRegistro(nota.getFechaRegistro());
        r.setVigencia(ReconocimientoCalificacion.VIGENTE);
        return reconocimientos.saveAndFlush(r);
    }

    // ── Reglas ──

    /**
     * Mismo curso, año, grado y periodo, y la nota cubre al menos las
     * competencias de destino: así no se da nota en una competencia no evaluada.
     */
    static boolean compatibles(Evaluacion origen, Asignacion destino, byte periodo, Set<Integer> competencias) {
        GradoSeccion de = seccion(origen);
        GradoSeccion a = destino.getGradoSeccion();
        return origen.getAsignacion().getCurso().getIdCurso().equals(destino.getCurso().getIdCurso())
                && origen.getPeriodo() == periodo
                && !competencias.isEmpty() && ids(origen).containsAll(competencias)
                && origen.getAsignacion().getAnioEscolar().getIdAnio().equals(destino.getAnioEscolar().getIdAnio())
                && de.getGrado().getNivel().getIdNivel().equals(a.getGrado().getNivel().getIdNivel())
                && Objects.equals(de.getGrado().getOrden(), a.getGrado().getOrden());
    }

    /** Por qué la nota no se puede usar en esa evaluación; null si se puede. */
    private String impedimento(NotaEvaluacion n, Evaluacion destino) {
        if (!esActivo(destino.getAcceso()) || !esActivo(destino.getAsignacion().getAcceso())
                || destino.getCompetencias().stream().anyMatch(c -> !esActivo(c.getAcceso()))) {
            return "La evaluación de destino está inactiva o eliminada";
        }
        String impedimento = impedimento(n, destino.getAsignacion(), destino.getPeriodo(), ids(destino),
                lectura(n.getMatricula(), destino.getAsignacion()));
        if (impedimento != null) {
            return impedimento;
        }
        boolean tieneNota = notas.findByEvaluacionIdEvaluacionAndAccesoNot(destino.getIdEvaluacion(), eliminado()).stream()
                .anyMatch(x -> x.getMatricula().getIdMatricula().equals(n.getMatricula().getIdMatricula()));
        return tieneNota ? "El alumno ya tiene nota en esa evaluación" : null;
    }

    private String impedimento(NotaEvaluacion n, Asignacion destino, byte periodo, Set<Integer> competencias,
                               LecturaAcademica lectura) {
        String impedimento = bloqueoOrigen(n, destino);
        if (impedimento != null) {
            return impedimento;
        }
        if (!compatibles(n.getEvaluacion(), destino, periodo, competencias)
                || escala(n.getEvaluacion()) != escala(destino)) {
            return "La nota no cubre esas competencias o es de otro curso, periodo o escala";
        }
        impedimento = bloqueoPeriodoFinal(destino, periodo, competencias, lectura);
        if (impedimento != null) {
            return impedimento;
        }
        boolean usada = reconocimientos.existsByNotaOrigenIdNotaEvaluacionAndSeccionDestinoIdGradoSeccionAndVigencia(
                n.getIdNotaEvaluacion(), destino.getGradoSeccion().getIdGradoSeccion(), ReconocimientoCalificacion.VIGENTE);
        return usada ? "Esa nota ya se usó en esta sección" : null;
    }

    private String bloqueoOrigen(NotaEvaluacion n, Asignacion actual) {
        if (reconocimientos.existsByNotaDestinoIdNotaEvaluacionAndVigencia(n.getIdNotaEvaluacion(), ReconocimientoCalificacion.VIGENTE)) {
            return "Es una nota usada de otra sección: elige la original";
        }
        return bloqueoOrigenVerificado(n, actual);
    }

    private String bloqueoOrigenVerificado(NotaEvaluacion n, Asignacion actual) {
        Evaluacion e = n.getEvaluacion();
        if (!esActivo(n.getAcceso()) || !esActivo(e.getAcceso()) || !esActivo(e.getAsignacion().getAcceso())
                || e.getCompetencias().stream().anyMatch(c -> !esActivo(c.getAcceso()))) {
            return "La evaluación original está inactiva o eliminada: solo consulta";
        }
        if (enSeccion(n, actual.getGradoSeccion())) {
            return "La nota original ya pertenece a esta sección";
        }
        return null;
    }

    private String bloqueoPeriodoFinal(Asignacion a, byte periodo, Set<Integer> competencias, LecturaAcademica lectura) {
        if (a.getAnioEscolar().getEstado() != AnioEscolarService.ESTADO_VIGENTE) {
            return "El año no está vigente";
        }
        boolean abierto = lectura.periodos().stream()
                .anyMatch(p -> p.getNumero() == periodo && PeriodoEvaluacion.ABIERTO.equals(p.estadoEn(LocalDate.now())));
        if (!abierto) {
            return "El periodo no está abierto: solo consulta";
        }
        boolean conFinal = lectura.finales().stream()
                .filter(n -> n.getBimestre() == periodo)
                .anyMatch(n -> EvaluacionService.afectaCompetencias(n, competencias));
        return conFinal ? "El alumno ya tiene nota final en esas competencias" : null;
    }

    private void validarMatricula(Matricula m, Asignacion a) {
        if (!esActivo(m.getAcceso()) || !m.getGradoSeccion().getIdGradoSeccion().equals(a.getGradoSeccion().getIdGradoSeccion())) {
            throw new IllegalArgumentException("El alumno ya no está matriculado en esta sección");
        }
    }

    private void validarCurso(NotaEvaluacion n, Asignacion a) {
        if (!mismoCursoYAnio(n, a)) {
            throw new IllegalArgumentException("La nota no corresponde al curso y año autorizados");
        }
    }

    private Set<Integer> alumnosConNota(Integer idEvaluacion, Asignacion asignacion) {
        Evaluacion e = evaluacion(idEvaluacion);
        if (!e.getAsignacion().getIdAsignacion().equals(asignacion.getIdAsignacion())) {
            throw new ResourceNotFoundException("Evaluación no encontrada en este curso");
        }
        return notas.findByEvaluacionIdEvaluacionAndAccesoNot(idEvaluacion, eliminado()).stream()
                .map(n -> n.getMatricula().getIdMatricula()).collect(Collectors.toSet());
    }

    // ── Respuestas ──

    private HistorialCalificacionesResponse aHistorial(NotaEvaluacion n, Asignacion actual,
                                                       List<ReconocimientoCalificacion> movimientos,
                                                       Integer usuario, Integer idRolActivo, LecturaAcademica lectura) {
        Evaluacion e = n.getEvaluacion();
        Curso c = e.getAsignacion().getCurso();
        return new HistorialCalificacionesResponse(n.getIdNotaEvaluacion(), nombreSeccion(seccion(e)), c.getNombre(),
                c.getCursoPadre() == null ? null : c.getCursoPadre().getNombre(), e.getNombre(), competencias(e),
                e.getPeriodo(), e.getFecha(), n.getCalificacion(), bloqueoOrigenVerificado(n, actual),
                movimientos.stream()
                        .filter(r -> r.getNotaOrigen().getIdNotaEvaluacion().equals(n.getIdNotaEvaluacion()))
                        .map(r -> respuesta(r, actual, usuario, idRolActivo, lectura))
                        .toList());
    }

    private ReconocimientoCalificacionResponse respuesta(ReconocimientoCalificacion r, Asignacion actual,
                                                         Integer usuario, Integer idRolActivo, LecturaAcademica lectura) {
        boolean enActual = r.getSeccionDestino().getIdGradoSeccion().equals(actual.getGradoSeccion().getIdGradoSeccion());
        Evaluacion destino = r.getNotaDestino().getEvaluacion();
        boolean puede = vigente(r) && enActual
                && bloqueoPeriodoFinal(destino.getAsignacion(), destino.getPeriodo(), ids(destino), lectura) == null
                && contexto.tieneAccion("EVALUACIONES", "ACTUALIZAR", usuario, idRolActivo);
        return new ReconocimientoCalificacionResponse(r.getIdReconocimiento(), destino.getIdEvaluacion(),
                destino.getNombre(), nombreSeccion(r.getSeccionDestino()), r.getCalificacion(),
                vigente(r) ? ReconocimientoCalificacion.ACTIVO : ReconocimientoCalificacion.ANULADO,
                nombre(r.getUsuarioRegistro()), r.getFechaRegistro(), r.getMotivoAnulacion(),
                r.getUsuarioAnulacion() == null ? null : nombre(r.getUsuarioAnulacion()), r.getFechaAnulacion(),
                puede, enActual);
    }

    private static TrasladoResponse.Opcion aOpcion(NotaEvaluacion n) {
        Evaluacion e = n.getEvaluacion();
        return new TrasladoResponse.Opcion(n.getIdNotaEvaluacion(), nombreSeccion(seccion(e)), e.getNombre(),
                e.getFecha(), n.getCalificacion(), competencias(e));
    }

    private static List<EvaluacionResponse.Competencia> competencias(Evaluacion e) {
        return e.getCompetencias().stream().sorted(Comparator.comparing(Competencia::getOrden))
                .map(x -> new EvaluacionResponse.Competencia(x.getIdCompetencia(), x.getCodigo(), x.getNombre()))
                .toList();
    }

    // ── Apoyo ──

    private LecturaAcademica lectura(Matricula m, Asignacion a) {
        return new LecturaAcademica(configuracion.periodos(a.getAnioEscolar(), a.getGradoSeccion().getGrado().getNivel()),
                finales.findByMatriculaIdMatriculaAndAccesoNot(m.getIdMatricula(), eliminado()));
    }

    private EscalaCalificacion escala(Evaluacion e) {
        return escala(e.getAsignacion());
    }

    private EscalaCalificacion escala(Asignacion a) {
        return configuracion.obtener(a.getAnioEscolar().getIdAnio(), a.getGradoSeccion().getGrado().getNivel().getIdNivel())
                .getEscala();
    }

    private NotaEvaluacion nota(Integer id) {
        return notas.findById(id).orElseThrow(() -> new ResourceNotFoundException("Nota no encontrada"));
    }

    private Evaluacion evaluacion(Integer id) {
        return evaluaciones.findById(id).orElseThrow(() -> new ResourceNotFoundException("Evaluación no encontrada"));
    }

    private Usuario usuario(Integer id) {
        return usuarios.findById(id).orElseThrow(() -> new IllegalArgumentException("No se pudo identificar al usuario"));
    }

    private static boolean mismoCursoYAnio(NotaEvaluacion n, Asignacion a) {
        Asignacion de = n.getEvaluacion().getAsignacion();
        return de.getCurso().getIdCurso().equals(a.getCurso().getIdCurso())
                && de.getAnioEscolar().getIdAnio().equals(a.getAnioEscolar().getIdAnio());
    }

    private static boolean enSeccion(NotaEvaluacion n, GradoSeccion gs) {
        return seccion(n.getEvaluacion()).getIdGradoSeccion().equals(gs.getIdGradoSeccion());
    }

    private static boolean esOrigen(NotaEvaluacion n, List<ReconocimientoCalificacion> movimientos) {
        return movimientos.stream().anyMatch(r -> r.getNotaOrigen().getIdNotaEvaluacion().equals(n.getIdNotaEvaluacion()));
    }

    private static boolean vigente(ReconocimientoCalificacion r) {
        return Byte.valueOf(ReconocimientoCalificacion.VIGENTE).equals(r.getVigencia());
    }

    private static Set<Integer> ids(Evaluacion e) {
        return e.getCompetencias().stream().map(Competencia::getIdCompetencia).collect(Collectors.toSet());
    }

    private static GradoSeccion seccion(Evaluacion e) {
        return e.getAsignacion().getGradoSeccion();
    }

    static String nombreSeccion(GradoSeccion s) {
        return MatriculaService.gradoYSeccion(s) + " · " + s.getTurno().getNombre();
    }

    private static String nombre(Usuario u) {
        return u.getNombre() + " " + u.getApellidoPat();
    }

    private static boolean esActivo(Acceso a) {
        return AccesoConstants.ACTIVO.equals(a.getIdAcceso());
    }

    private Acceso activo() {
        return accesos.findById(AccesoConstants.ACTIVO).orElseThrow(() -> new IllegalStateException("Falta el acceso Activo"));
    }

    private Acceso eliminado() {
        return accesos.findById(AccesoConstants.ELIMINADO).orElseThrow(() -> new IllegalStateException("Falta el acceso Eliminado"));
    }
}
