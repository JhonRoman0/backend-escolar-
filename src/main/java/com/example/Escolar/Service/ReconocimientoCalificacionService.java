package com.example.Escolar.Service;

import com.example.Escolar.Dto.*;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public List<HistorialCalificacionesResponse> historial(Integer idMatricula, Integer idAsignacion,
                                                          Integer usuario, List<String> roles) {
        Asignacion actual = evaluacionService.asignacionAccesible(idAsignacion, usuario, roles);
        Matricula m = matriculas.findById(idMatricula).orElseThrow();
        validarMatricula(m, actual);
        List<ReconocimientoCalificacion> movimientos = reconocimientos.findByNotaOrigenMatriculaIdMatricula(idMatricula);
        var lectura = lectura(m, actual);
        return notas.findByMatriculaIdMatriculaAndAccesoNot(idMatricula, eliminado()).stream()
            .filter(n -> movimientos.stream().noneMatch(r -> r.getNotaDestino().getIdNotaEvaluacion().equals(n.getIdNotaEvaluacion())))
            .filter(n -> n.getEvaluacion().getAsignacion().getCurso().getIdCurso().equals(actual.getCurso().getIdCurso()))
            .filter(n -> n.getEvaluacion().getAsignacion().getAnioEscolar().getIdAnio().equals(actual.getAnioEscolar().getIdAnio()))
            .filter(n -> !seccion(n.getEvaluacion()).getIdGradoSeccion().equals(m.getGradoSeccion().getIdGradoSeccion())
                || movimientos.stream().anyMatch(r -> r.getNotaOrigen().getIdNotaEvaluacion().equals(n.getIdNotaEvaluacion())))
            .sorted(Comparator.comparing((NotaEvaluacion n) -> n.getEvaluacion().getFecha()).reversed())
            .map(n -> {
                Evaluacion e = n.getEvaluacion(); Curso c = e.getAsignacion().getCurso();
                return new HistorialCalificacionesResponse(n.getIdNotaEvaluacion(), nombreSeccion(seccion(e)), c.getNombre(),
                    c.getCursoPadre() == null ? null : c.getCursoPadre().getNombre(), e.getNombre(),
                    e.getCompetencias().stream().sorted(Comparator.comparing(Competencia::getOrden))
                      .map(x -> new EvaluacionResponse.Competencia(x.getIdCompetencia(), x.getCodigo(), x.getNombre())).toList(),
                    e.getPeriodo(), e.getFecha(), n.getCalificacion(), bloqueoOrigen(n, actual, true),
                    movimientos.stream().filter(r -> r.getNotaOrigen().getIdNotaEvaluacion().equals(n.getIdNotaEvaluacion()))
                      .map(r -> respuesta(r, actual, usuario, roles, lectura)).toList());
            }).toList();
    }

    @Transactional(readOnly = true)
    public List<ReconocimientoCalificacionResponse.Candidata> candidatas(Integer idNota, Integer idAsignacion,
                                                                       Integer usuario, List<String> roles) {
        Asignacion actual = evaluacionService.asignacionAccesible(idAsignacion, usuario, roles);
        NotaEvaluacion origen = notas.findById(idNota).orElseThrow();
        validarMatricula(origen.getMatricula(), actual);
        validarCurso(origen, actual);
        var lectura = lectura(origen.getMatricula(), actual);
        return evaluaciones.findByAsignacionIdAsignacionAndPeriodoAndAccesoNotOrderByFechaAscIdEvaluacionAsc(
                idAsignacion, origen.getEvaluacion().getPeriodo(), eliminado()).stream()
            .filter(e -> compatibles(origen.getEvaluacion(), e))
            .map(e -> new ReconocimientoCalificacionResponse.Candidata(e.getIdEvaluacion(), e.getNombre(),
                motivo(origen, e, lectura))).toList();
    }

    @Transactional
    public ReconocimientoCalificacionResponse reconocer(ReconocimientoCalificacionRequest request,
                                                        Integer usuario, List<String> roles) {
        NotaEvaluacion origen = notas.findById(request.idNotaOrigen()).orElseThrow();
        bloqueo.matricula(origen.getMatricula().getIdMatricula());
        bloqueo.nota(origen.getIdNotaEvaluacion());
        Evaluacion destino = evaluaciones.findById(request.idEvaluacionDestino()).orElseThrow();
        List.of(origen.getEvaluacion().getAsignacion().getIdAsignacion(), destino.getAsignacion().getIdAsignacion())
            .stream().distinct().sorted().forEach(bloqueo::asignacion);
        List.of(origen.getEvaluacion().getIdEvaluacion(), destino.getIdEvaluacion())
            .stream().distinct().sorted().forEach(bloqueo::evaluacion);
        evaluacionService.asignacionAccesible(destino.getAsignacion().getIdAsignacion(), usuario, roles);
        validarMatricula(origen.getMatricula(), destino.getAsignacion());
        validarCurso(origen, destino.getAsignacion());
        String impedimento = motivo(origen, destino);
        if (impedimento != null) throw new IllegalArgumentException(impedimento);
        if (!origen.getCalificacion().equals(request.calificacionOrigen()))
            throw new IllegalArgumentException("La nota original cambió; actualiza el historial");
        NotaEvaluacion nota = notas.findByEvaluacionIdEvaluacion(destino.getIdEvaluacion()).stream()
            .filter(n -> n.getMatricula().getIdMatricula().equals(origen.getMatricula().getIdMatricula()))
            .findFirst().orElseGet(NotaEvaluacion::new);
        nota.setMatricula(origen.getMatricula()); nota.setEvaluacion(destino);
        nota.setCalificacion(Calificaciones.normalizar(escala(destino), origen.getCalificacion()));
        nota.setUsuarioRegistro(usuarios.findById(usuario).orElseThrow());
        nota.setFechaRegistro(LocalDateTime.now()); nota.setAcceso(activo());
        nota = notas.saveAndFlush(nota);
        ReconocimientoCalificacion r = new ReconocimientoCalificacion();
        r.setNotaOrigen(origen); r.setNotaDestino(nota); r.setSeccionDestino(seccion(destino));
        r.setCalificacion(nota.getCalificacion()); r.setEscala(escala(destino));
        r.setUsuarioRegistro(nota.getUsuarioRegistro()); r.setFechaRegistro(nota.getFechaRegistro()); r.setVigencia(ReconocimientoCalificacion.VIGENTE);
        return respuesta(reconocimientos.saveAndFlush(r), destino.getAsignacion(), usuario, roles);
    }

    @Transactional
    public void anular(Integer id, String motivo, Integer usuario, List<String> roles) {
        ReconocimientoCalificacion r = reconocimientos.findById(id).orElseThrow();
        bloqueo.matricula(r.getNotaOrigen().getMatricula().getIdMatricula());
        bloqueo.reconocimiento(id);
        Evaluacion e = r.getNotaDestino().getEvaluacion();
        bloqueo.asignacion(e.getAsignacion().getIdAsignacion()); bloqueo.evaluacion(e.getIdEvaluacion());
        evaluacionService.asignacionAccesible(e.getAsignacion().getIdAsignacion(), usuario, roles);
        validarMatricula(r.getNotaDestino().getMatricula(), e.getAsignacion());
        if (r.getVigencia() == null) throw new IllegalArgumentException("El reconocimiento ya está anulado");
        if (motivo == null || motivo.isBlank() || motivo.trim().length() > ReconocimientoCalificacion.MOTIVO_MAXIMO)
            throw new IllegalArgumentException("Escribe un motivo de hasta 500 caracteres");
        String impedimento = bloqueoPeriodoFinal(r.getNotaDestino().getMatricula(), e);
        if (impedimento != null) throw new IllegalArgumentException(impedimento);
        r.getNotaDestino().setAcceso(eliminado()); notas.save(r.getNotaDestino());
        r.setVigencia(null); r.setUsuarioAnulacion(usuarios.findById(usuario).orElseThrow());
        r.setFechaAnulacion(LocalDateTime.now()); r.setMotivoAnulacion(motivo.trim());
        reconocimientos.saveAndFlush(r);
    }

    static boolean compatibles(Evaluacion origen, Evaluacion destino) {
        return origen.getAsignacion().getCurso().getIdCurso().equals(destino.getAsignacion().getCurso().getIdCurso())
            && origen.getPeriodo() == destino.getPeriodo() && !ids(origen).isEmpty() && ids(origen).equals(ids(destino))
            && origen.getAsignacion().getAnioEscolar().getIdAnio().equals(destino.getAsignacion().getAnioEscolar().getIdAnio())
            && seccion(origen).getGrado().getNivel().getIdNivel().equals(seccion(destino).getGrado().getNivel().getIdNivel())
            && Objects.equals(seccion(origen).getGrado().getOrden(), seccion(destino).getGrado().getOrden());
    }
    private static Set<Integer> ids(Evaluacion e) {
        return e.getCompetencias().stream().map(Competencia::getIdCompetencia).collect(Collectors.toSet());
    }
    private String motivo(NotaEvaluacion n, Evaluacion destino) {
        return motivo(n, destino, lectura(n.getMatricula(), destino.getAsignacion()));
    }
    private String motivo(NotaEvaluacion n, Evaluacion destino, LecturaAcademica lectura) {
        String impedimento = bloqueoOrigen(n, destino.getAsignacion());
        if (impedimento != null) return impedimento;
        if (!compatibles(n.getEvaluacion(), destino) || escala(n.getEvaluacion()) != escala(destino))
            return "Curso, competencias, periodo o escala incompatibles";
        if (!esActivo(destino.getAcceso()) || !esActivo(destino.getAsignacion().getAcceso())
            || destino.getCompetencias().stream().anyMatch(c -> !esActivo(c.getAcceso()))) return "Destino inactivo o eliminado";
        impedimento = bloqueoPeriodoFinal(n.getMatricula(), destino, lectura);
        if (impedimento != null) return impedimento;
        if (reconocimientos.existsByNotaOrigenIdNotaEvaluacionAndSeccionDestinoIdGradoSeccionAndVigencia(
            n.getIdNotaEvaluacion(), seccion(destino).getIdGradoSeccion(), ReconocimientoCalificacion.VIGENTE)) return "Ya reconocido en esta sección";
        if (notas.findByEvaluacionIdEvaluacionAndAccesoNot(destino.getIdEvaluacion(), eliminado()).stream()
            .anyMatch(x -> x.getMatricula().getIdMatricula().equals(n.getMatricula().getIdMatricula()))) return "La evaluación de destino ya tiene calificación";
        return null;
    }
    private String bloqueoOrigen(NotaEvaluacion n, Asignacion actual) {
        return bloqueoOrigen(n, actual, false);
    }
    private String bloqueoOrigen(NotaEvaluacion n, Asignacion actual, boolean originalVerificado) {
        if (!originalVerificado && reconocimientos.existsByNotaDestinoIdNotaEvaluacion(n.getIdNotaEvaluacion())) return "Selecciona la nota original, no un reconocimiento";
        if (!esActivo(n.getAcceso()) || !esActivo(n.getEvaluacion().getAcceso())
            || !esActivo(n.getEvaluacion().getAsignacion().getAcceso())
            || n.getEvaluacion().getCompetencias().stream().anyMatch(c -> !esActivo(c.getAcceso()))) return "Origen inactivo o eliminado; solo consulta";
        if (seccion(n.getEvaluacion()).getIdGradoSeccion().equals(actual.getGradoSeccion().getIdGradoSeccion()))
            return "La calificación original ya pertenece a esta sección";
        return null;
    }
    private record LecturaAcademica(List<PeriodoEvaluacion> periodos, List<Nota> finales) {}
    private LecturaAcademica lectura(Matricula m, Asignacion a) {
        return new LecturaAcademica(configuracion.periodos(a.getAnioEscolar(), a.getGradoSeccion().getGrado().getNivel()), finales.findByMatriculaIdMatriculaAndAccesoNot(m.getIdMatricula(), eliminado()));
    }
    private String bloqueoPeriodoFinal(Matricula m, Evaluacion e) {
        return bloqueoPeriodoFinal(m, e, lectura(m, e.getAsignacion()));
    }
    private String bloqueoPeriodoFinal(Matricula m, Evaluacion e, LecturaAcademica lectura) {
        if (e.getAsignacion().getAnioEscolar().getEstado() != AnioEscolarService.ESTADO_VIGENTE) return "El año no está vigente";
        boolean abierto = lectura.periodos().stream()
            .anyMatch(p -> p.getNumero() == e.getPeriodo() && PeriodoEvaluacion.ABIERTO.equals(p.estadoEn(LocalDate.now())));
        if (!abierto) return "El periodo no está abierto; solo consulta";
        if (lectura.finales().stream().filter(n -> n.getBimestre() == e.getPeriodo()).anyMatch(n -> EvaluacionService.afectaCompetencias(n, ids(e)))) return "Nota final registrada; reconocimiento no disponible";
        return null;
    }
    private ReconocimientoCalificacionResponse respuesta(ReconocimientoCalificacion r, Asignacion actual, Integer usuario, List<String> roles) {
        return respuesta(r, actual, usuario, roles, lectura(r.getNotaOrigen().getMatricula(), actual));
    }
    private ReconocimientoCalificacionResponse respuesta(ReconocimientoCalificacion r, Asignacion actual, Integer usuario, List<String> roles, LecturaAcademica lectura) {
        boolean puede = r.getVigencia() != null && r.getSeccionDestino().getIdGradoSeccion().equals(actual.getGradoSeccion().getIdGradoSeccion())
            && bloqueoPeriodoFinal(r.getNotaDestino().getMatricula(), r.getNotaDestino().getEvaluacion(), lectura) == null
            && contexto.tieneAccion("EVALUACIONES", "ACTUALIZAR", usuario, roles);
        return new ReconocimientoCalificacionResponse(r.getIdReconocimiento(), r.getNotaDestino().getEvaluacion().getIdEvaluacion(),
            r.getNotaDestino().getEvaluacion().getNombre(), nombreSeccion(r.getSeccionDestino()), r.getCalificacion(),
            r.getVigencia() == null ? ReconocimientoCalificacion.ANULADO : ReconocimientoCalificacion.ACTIVO, nombre(r.getUsuarioRegistro()), r.getFechaRegistro(),
            r.getMotivoAnulacion(), r.getUsuarioAnulacion() == null ? null : nombre(r.getUsuarioAnulacion()), r.getFechaAnulacion(), puede, r.getSeccionDestino().getIdGradoSeccion().equals(actual.getGradoSeccion().getIdGradoSeccion()));
    }
    private void validarMatricula(Matricula m, Asignacion a) {
        if (!esActivo(m.getAcceso()) || !m.getGradoSeccion().getIdGradoSeccion().equals(a.getGradoSeccion().getIdGradoSeccion()))
            throw new IllegalArgumentException("El alumno ya no está matriculado en esta sección");
    }
    private void validarCurso(NotaEvaluacion n, Asignacion a) {
        if (!n.getEvaluacion().getAsignacion().getCurso().getIdCurso().equals(a.getCurso().getIdCurso())
            || !n.getEvaluacion().getAsignacion().getAnioEscolar().getIdAnio().equals(a.getAnioEscolar().getIdAnio()))
            throw new IllegalArgumentException("La nota no corresponde al curso y año autorizados");
    }
    private EscalaCalificacion escala(Evaluacion e) {
        return configuracion.obtener(e.getAsignacion().getAnioEscolar().getIdAnio(), seccion(e).getGrado().getNivel().getIdNivel()).getEscala();
    }
    private static GradoSeccion seccion(Evaluacion e) { return e.getAsignacion().getGradoSeccion(); }
    private static String nombreSeccion(GradoSeccion s) { return MatriculaService.gradoYSeccion(s) + " · " + s.getTurno().getNombre(); }
    private static String nombre(Usuario u) { return u.getNombre() + " " + u.getApellidoPat(); }
    private boolean esActivo(Acceso a) { return AccesoConstants.ACTIVO.equals(a.getIdAcceso()); }
    private Acceso activo() { return accesos.findById(AccesoConstants.ACTIVO).orElseThrow(); }
    private Acceso eliminado() { return accesos.findById(AccesoConstants.ELIMINADO).orElseThrow(); }
}
