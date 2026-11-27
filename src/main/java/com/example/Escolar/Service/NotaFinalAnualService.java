package com.example.Escolar.Service;

import com.example.Escolar.Dto.ConfiguracionAcademicaResponse;
import com.example.Escolar.Dto.EvaluacionResponse;
import com.example.Escolar.Dto.NotaAnualRequest;
import com.example.Escolar.Dto.NotaAnualResponse;
import com.example.Escolar.Dto.NotaFinalRequest;
import com.example.Escolar.Dto.NotaFinalResponse;
import com.example.Escolar.Dto.ReaperturaNotaRequest;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.NotaFinalAnualRepository;
import com.example.Escolar.Repository.NotaRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Service.NotaFinalService.Alcance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Nota final del año por competencia. Se pone cuando el último periodo ya
 * cerró y la competencia tiene la nota final de todos los periodos. En literal
 * la elige quien califica (con conclusión si es B o C); en vigesimal es el
 * promedio de los periodos. Mismo alcance que las notas de cada periodo.
 */
@Service
@RequiredArgsConstructor
public class NotaFinalAnualService {

    static final String PERMISO = "NOTAS_ANUALES";

    private final NotaFinalService notaFinalService;
    private final NotaRepository notaRepository;
    private final NotaFinalAnualRepository anualRepository;
    private final ConfiguracionAcademicaService configuracionService;
    private final UsuarioRepository usuarioRepository;
    private final AccesoRepository accesoRepository;
    private final AccesoContextoService accesoContextoService;
    private final BloqueoCalificaciones bloqueo;

    /** Lo que se lee una vez por pantalla o guardado. */
    private record Contexto(Alcance alcance, ConfiguracionAcademicaResponse config, List<PeriodoEvaluacion> periodos,
                            String noDisponible, List<Matricula> alumnos, Map<String, String> porPeriodo,
                            Map<String, NotaFinalAnual> anuales) {
    }

    // ── Consultas ──

    @Transactional(readOnly = true)
    public List<NotaFinalResponse.Curso> cursos(Integer idAnio, Integer idUsuario, List<String> roles) {
        return notaFinalService.cursos(idAnio, idUsuario, roles, PERMISO);
    }

    @Transactional(readOnly = true)
    public NotaAnualResponse.Detalle detalle(Integer idGradoSeccion, Integer idArea, Integer idUsuario, List<String> roles) {
        Contexto ctx = contexto(idGradoSeccion, idArea, idUsuario, roles, AccesoContextoService.Operacion.LECTURA);
        boolean vigesimal = ctx.config().getEscala() == EscalaCalificacion.VIGESIMAL;
        List<NotaAnualResponse.Alumno> filas = ctx.alumnos().stream().map(m -> {
            Alumno a = m.getAlumnoApoderado().getAlumno();
            List<NotaAnualResponse.Celda> celdas = ctx.alcance().competencias().stream().map(c -> {
                List<NotaAnualResponse.NotaPeriodo> periodos = ctx.periodos().stream()
                        .map(p -> new NotaAnualResponse.NotaPeriodo(p.getNumero(),
                                ctx.porPeriodo().get(clavePeriodo(m, c, p.getNumero()))))
                        .toList();
                List<String> faltan = faltan(ctx, m, c);
                NotaFinalAnual anual = ctx.anuales().get(NotaFinalService.clave(m.getIdMatricula(), c.getIdCompetencia()));
                return new NotaAnualResponse.Celda(c.getIdCompetencia(), periodos, faltan,
                        anual != null ? anual.getCalificacion() : null,
                        anual != null ? anual.getConclusionDescriptiva() : null,
                        anual != null ? NotaFinalService.nombre(anual.getUsuarioRegistro()) : null,
                        vigesimal && faltan.isEmpty() ? promedio(ctx, m, c) : null);
            }).toList();
            return new NotaAnualResponse.Alumno(m.getIdMatricula(), a.getCodigo(),
                    a.getApellidoPat() + " " + a.getApellidoMat() + ", " + a.getNombre(), celdas);
        }).toList();

        boolean disponible = ctx.noDisponible() == null;
        boolean editable = disponible && notaFinalService.puedeEditar(ctx.alcance(), idUsuario, roles, PERMISO);
        return new NotaAnualResponse.Detalle(notaFinalService.aCurso(ctx.alcance()), ctx.config().getEscala(),
                ctx.periodos().stream()
                        .map(p -> new NotaAnualResponse.Periodo(p.getNumero(), ctx.config().getTipoPeriodo().nombre(p.getNumero())))
                        .toList(),
                disponible, ctx.noDisponible(),
                editable && accesoContextoService.tieneAccion(PERMISO, "CREAR", idUsuario, roles),
                editable && accesoContextoService.tieneAccion(PERMISO, "ACTUALIZAR", idUsuario, roles),
                ctx.alcance().competencias().stream()
                        .map(c -> new EvaluacionResponse.Competencia(c.getIdCompetencia(), c.getCodigo(), c.getNombre()))
                        .toList(),
                filas);
    }

    // ── Escritura ──

    /** Solo notas nuevas: una ya registrada se cambia con corregir. */
    @Transactional
    public NotaAnualResponse.Detalle registrar(NotaAnualRequest request, Integer idUsuario, List<String> roles) {
        return guardar(request, false, idUsuario, roles);
    }

    /** En literal cambia la nota o la conclusión con motivo; en vigesimal solo se reabre. */
    @Transactional
    public NotaAnualResponse.Detalle corregir(NotaAnualRequest request, Integer idUsuario, List<String> roles) {
        return guardar(request, true, idUsuario, roles);
    }

    /** Anula la nota del año con su motivo; luego se pueden reabrir las de los periodos. */
    @Transactional
    public NotaAnualResponse.Detalle reabrir(ReaperturaNotaRequest request, Integer idUsuario, List<String> roles) {
        bloqueo.matricula(request.idMatricula());
        Contexto ctx = contexto(request.idGradoSeccion(), request.idArea(), idUsuario, roles, AccesoContextoService.Operacion.ESCRITURA);
        vigente(ctx);
        Matricula m = alumno(ctx, request.idMatricula());
        Competencia c = competencia(ctx, request.idCompetencia());
        NotaFinalAnual actual = ctx.anuales().get(NotaFinalService.clave(m.getIdMatricula(), c.getIdCompetencia()));
        if (actual == null) {
            throw new IllegalArgumentException("No hay nota final del año que reabrir");
        }
        anular(actual, request.motivo().trim(), usuario(idUsuario));
        return detalle(request.idGradoSeccion(), request.idArea(), idUsuario, roles);
    }

    private NotaAnualResponse.Detalle guardar(NotaAnualRequest request, boolean correccion,
                                              Integer idUsuario, List<String> roles) {
        request.notas().stream().map(NotaFinalRequest.Item::idMatricula).distinct().sorted().forEach(bloqueo::matricula);
        Contexto ctx = contexto(request.idGradoSeccion(), request.idArea(), idUsuario, roles, AccesoContextoService.Operacion.ESCRITURA);
        if (ctx.noDisponible() != null) {
            throw new IllegalArgumentException(ctx.noDisponible());
        }
        EscalaCalificacion escala = ctx.config().getEscala();
        Usuario registrador = usuario(idUsuario);
        String motivo = request.motivo() == null ? "" : request.motivo().trim();
        Acceso activo = accesoRepository.findById(AccesoConstants.ACTIVO)
                .orElseThrow(() -> new IllegalStateException("Falta el acceso Activo"));

        for (NotaFinalRequest.Item item : request.notas()) {
            Matricula m = alumno(ctx, item.idMatricula());
            Competencia c = competencia(ctx, item.idCompetencia());
            String quien = NotaFinalService.nombreAlumno(m) + " en la competencia " + c.getCodigo();
            NotaFinalAnual actual = ctx.anuales().get(NotaFinalService.clave(m.getIdMatricula(), c.getIdCompetencia()));
            if (actual != null && !correccion) {
                throw new IllegalArgumentException("La nota del año de " + quien + " ya la registró "
                        + NotaFinalService.nombre(actual.getUsuarioRegistro()) + ": cambiarla requiere el permiso de corregir");
            }
            List<String> faltan = faltan(ctx, m, c);
            if (!faltan.isEmpty()) {
                throw new IllegalArgumentException("A " + quien + " le falta la nota final de: " + String.join(", ", faltan));
            }
            String calificacion;
            String conclusion;
            if (escala == EscalaCalificacion.VIGESIMAL) {
                // Promedio de los periodos; la ya registrada solo cambia reabriéndola.
                calificacion = actual != null ? actual.getCalificacion() : String.valueOf(promedio(ctx, m, c));
                conclusion = null;
            } else {
                if (item.calificacion() == null || item.calificacion().isBlank()) {
                    throw new IllegalArgumentException("Falta la nota de " + quien + ": para quitar una nota, reábrela");
                }
                calificacion = Calificaciones.normalizar(escala, item.calificacion());
                conclusion = Calificaciones.validarConclusion(escala, calificacion, item.conclusionDescriptiva(), quien);
            }
            if (actual != null) {
                if (calificacion.equals(actual.getCalificacion()) && Objects.equals(conclusion, actual.getConclusionDescriptiva())) {
                    continue;
                }
                if (motivo.isEmpty()) {
                    throw new IllegalArgumentException("Escribe el motivo de la corrección");
                }
                anular(actual, motivo, registrador);
            }
            NotaFinalAnual nota = new NotaFinalAnual();
            nota.setMatricula(m);
            nota.setCompetencia(c);
            nota.setCalificacion(calificacion);
            nota.setConclusionDescriptiva(conclusion);
            nota.setFechaRegistro(LocalDate.now());
            nota.setUsuarioRegistro(registrador);
            nota.setAcceso(activo);
            anualRepository.save(nota);
        }
        return detalle(request.idGradoSeccion(), request.idArea(), idUsuario, roles);
    }

    // ── Apoyo ──

    private Contexto contexto(Integer idGradoSeccion, Integer idArea, Integer idUsuario, List<String> roles,
                              AccesoContextoService.Operacion operacion) {
        Alcance alcance = notaFinalService.alcance(idGradoSeccion, idArea, idUsuario, roles, PERMISO, operacion);
        GradoSeccion gs = alcance.gradoSeccion();
        ConfiguracionAcademicaResponse config = notaFinalService.configuracion(gs);
        List<PeriodoEvaluacion> periodos = configuracionService.periodos(gs.getAnioEscolar(), gs.getGrado().getNivel()).stream()
                .sorted(Comparator.comparing(PeriodoEvaluacion::getNumero))
                .toList();
        List<Matricula> alumnos = notaFinalService.alumnosDe(alcance, idUsuario, roles, PERMISO, operacion);
        Set<Integer> competencias = alcance.competencias().stream().map(Competencia::getIdCompetencia).collect(Collectors.toSet());
        List<Integer> ids = alumnos.stream().map(Matricula::getIdMatricula).toList();
        Acceso eliminado = notaFinalService.eliminado();
        Map<String, String> porPeriodo = alumnos.isEmpty() || competencias.isEmpty() ? Map.of()
                : notaRepository.findByMatriculaIdMatriculaInAndCompetenciaIdCompetenciaInAndAccesoNot(ids, competencias, eliminado)
                .stream()
                .collect(Collectors.toMap(n -> n.getMatricula().getIdMatricula() + "-" + n.getCompetencia().getIdCompetencia()
                        + "-" + n.getBimestre(), Nota::getCalificacion, (a, b) -> a));
        Map<String, NotaFinalAnual> anuales = alumnos.isEmpty() || competencias.isEmpty() ? Map.of()
                : anualRepository.findByMatriculaIdMatriculaInAndCompetenciaIdCompetenciaInAndAccesoNot(ids, competencias, eliminado)
                .stream()
                .collect(Collectors.toMap(n -> NotaFinalService.clave(n.getMatricula().getIdMatricula(),
                        n.getCompetencia().getIdCompetencia()), Function.identity(), (a, b) -> a));
        return new Contexto(alcance, config, periodos, noDisponible(gs, config, periodos), alumnos, porPeriodo, anuales);
    }

    /** Por qué aún no se pone la nota del año; null si ya se puede. */
    static String noDisponible(GradoSeccion gs, ConfiguracionAcademicaResponse config, List<PeriodoEvaluacion> periodos) {
        if (gs.getAnioEscolar().getEstado() != AnioEscolarService.ESTADO_VIGENTE) {
            return "Las notas del año solo se registran en un año vigente";
        }
        if (periodos.isEmpty()) {
            return "El nivel aún no tiene periodos";
        }
        PeriodoEvaluacion ultimo = periodos.get(periodos.size() - 1);
        if (!PeriodoEvaluacion.CERRADO.equals(ultimo.estadoEn(LocalDate.now()))) {
            return "Disponible cuando termine el " + config.getTipoPeriodo().nombre(ultimo.getNumero()).toLowerCase();
        }
        return null;
    }

    private void vigente(Contexto ctx) {
        if (ctx.alcance().gradoSeccion().getAnioEscolar().getEstado() != AnioEscolarService.ESTADO_VIGENTE) {
            throw new IllegalArgumentException("Las notas del año solo se reabren en un año vigente");
        }
    }

    private List<String> faltan(Contexto ctx, Matricula m, Competencia c) {
        return ctx.periodos().stream()
                .filter(p -> !ctx.porPeriodo().containsKey(clavePeriodo(m, c, p.getNumero())))
                .map(p -> ctx.config().getTipoPeriodo().nombre(p.getNumero()))
                .toList();
    }

    private Integer promedio(Contexto ctx, Matricula m, Competencia c) {
        return Calificaciones.promedio(ctx.periodos().stream()
                .map(p -> ctx.porPeriodo().get(clavePeriodo(m, c, p.getNumero())))
                .toList());
    }

    private static String clavePeriodo(Matricula m, Competencia c, int periodo) {
        return m.getIdMatricula() + "-" + c.getIdCompetencia() + "-" + periodo;
    }

    private static Matricula alumno(Contexto ctx, Integer idMatricula) {
        return ctx.alumnos().stream().filter(m -> m.getIdMatricula().equals(idMatricula)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Un alumno ya no está matriculado en esta sección"));
    }

    private static Competencia competencia(Contexto ctx, Integer idCompetencia) {
        return ctx.alcance().competencias().stream().filter(c -> c.getIdCompetencia().equals(idCompetencia)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No puedes calificar esa competencia en "
                        + ctx.alcance().area().getNombre()));
    }

    private void anular(NotaFinalAnual nota, String motivo, Usuario usuario) {
        nota.setAcceso(notaFinalService.eliminado());
        nota.setMotivoAnulacion(motivo);
        nota.setUsuarioAnulacion(usuario);
        nota.setFechaAnulacion(LocalDateTime.now());
        anualRepository.save(nota);
    }

    private Usuario usuario(Integer idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("No se pudo identificar al usuario"));
    }
}
