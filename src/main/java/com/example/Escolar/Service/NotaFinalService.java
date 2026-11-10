package com.example.Escolar.Service;

import com.example.Escolar.Dto.ConfiguracionAcademicaResponse;
import com.example.Escolar.Dto.EvaluacionResponse;
import com.example.Escolar.Dto.NotaFinalRequest;
import com.example.Escolar.Dto.NotaFinalResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Nota final de cada competencia por periodo. La pone el docente del área o de
 * un subcurso ligado a esa competencia; quien no es docente, con el permiso, en
 * cualquier sección. La primera nota registrada es la que vale: cambiarla pide
 * el permiso de corregir. Con el periodo cerrado se necesita además un código
 * de autorización.
 */
@Service
@RequiredArgsConstructor
public class NotaFinalService {

    private static final String PERMISO = "NOTAS";

    private final NotaRepository notaRepository;
    private final AsignacionRepository asignacionRepository;
    private final GradoSeccionRepository gradoSeccionRepository;
    private final CursoRepository cursoRepository;
    private final CompetenciaRepository competenciaRepository;
    private final MatriculaRepository matriculaRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final NotaEvaluacionRepository notaEvaluacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AccesoRepository accesoRepository;
    private final AccesoContextoService accesoContextoService;
    private final ConfiguracionAcademicaService configuracionService;
    private final NotaService notaService;
    private final BloqueoCalificaciones bloqueo;

    /** Un área de una sección y las competencias que el usuario puede calificar en ella. */
    private record Alcance(GradoSeccion gradoSeccion, Curso area, List<Competencia> competencias) {
    }

    // ── Consultas ──

    @Transactional(readOnly = true)
    public List<NotaFinalResponse.Curso> cursos(Integer idAnio, Integer idUsuario, List<String> roles) {
        return alcances(idAnio, idUsuario, roles).stream().map(this::aCurso).toList();
    }

    @Transactional(readOnly = true)
    public NotaFinalResponse.Detalle detalle(Integer idGradoSeccion, Integer idArea, Integer periodo,
                                             Integer idUsuario, List<String> roles) {
        Alcance alcance = alcance(idGradoSeccion, idArea, idUsuario, roles);
        ConfiguracionAcademicaResponse config = configuracion(alcance.gradoSeccion());
        PeriodoEvaluacion numero = periodo(alcance.gradoSeccion(), periodo);
        List<Matricula> alumnos = alumnosDe(alcance.gradoSeccion());
        Set<Integer> competencias = ids(alcance.competencias());

        Map<String, Nota> finales = finales(alumnos, competencias, periodo);
        List<Evaluacion> evaluaciones = evaluacionesDelArea(alcance, periodo);
        Map<Integer, List<NotaEvaluacion>> notasPorEvaluacion = notaEvaluacionRepository
                .findByEvaluacionIdEvaluacionInAndAccesoNot(evaluaciones.stream().map(Evaluacion::getIdEvaluacion).toList(), eliminado()).stream()
                .collect(Collectors.groupingBy(n -> n.getEvaluacion().getIdEvaluacion()));

        List<NotaFinalResponse.Alumno> filas = alumnos.stream().map(m -> {
            Alumno a = m.getAlumnoApoderado().getAlumno();
            List<NotaFinalResponse.Celda> celdas = alcance.competencias().stream().map(c -> {
                List<NotaFinalResponse.Guia> guia = evaluaciones.stream()
                        .filter(e -> e.getCompetencias().stream().anyMatch(x -> x.getIdCompetencia().equals(c.getIdCompetencia())))
                        .flatMap(e -> notasPorEvaluacion.getOrDefault(e.getIdEvaluacion(), List.of()).stream()
                                .filter(n -> n.getMatricula().getIdMatricula().equals(m.getIdMatricula()))
                                .map(n -> new NotaFinalResponse.Guia(e.getNombre(), e.getAsignacion().getCurso().getNombre(),
                                        e.getFecha(), n.getCalificacion())))
                        .sorted(Comparator.comparing(NotaFinalResponse.Guia::fecha))
                        .toList();
                Nota fin = finales.get(clave(m.getIdMatricula(), c.getIdCompetencia()));
                return new NotaFinalResponse.Celda(c.getIdCompetencia(),
                        fin != null ? fin.getCalificacion() : null,
                        fin != null ? fin.getConclusionDescriptiva() : null,
                        fin != null ? nombre(fin.getUsuarioRegistro()) : null,
                        guia,
                        config.getEscala() == EscalaCalificacion.VIGESIMAL
                                ? Calificaciones.promedio(guia.stream().map(NotaFinalResponse.Guia::calificacion).toList())
                                : null);
            }).toList();
            return new NotaFinalResponse.Alumno(m.getIdMatricula(), a.getCodigo(),
                    a.getApellidoPat() + " " + a.getApellidoMat() + ", " + a.getNombre(), celdas);
        }).toList();

        String estado = numero.estadoEn(LocalDate.now());
        boolean vigente = alcance.gradoSeccion().getAnioEscolar().getEstado() == AnioEscolarService.ESTADO_VIGENTE;
        boolean periodoEditable = vigente && !PeriodoEvaluacion.POR_COMENZAR.equals(estado);
        return new NotaFinalResponse.Detalle(aCurso(alcance), config.getEscala(),
                new NotaFinalResponse.Periodo(numero.getNumero(), config.getTipoPeriodo().nombre(numero.getNumero()), estado),
                periodoEditable,
                periodoEditable && accesoContextoService.tieneAccion(PERMISO, "CREAR", idUsuario, roles),
                periodoEditable && PeriodoEvaluacion.CERRADO.equals(estado),
                periodoEditable && accesoContextoService.tieneAccion(PERMISO, "ACTUALIZAR", idUsuario, roles),
                alcance.competencias().stream()
                        .map(c -> new NotaFinalResponse.CompetenciaFinal(c.getIdCompetencia(), c.getCodigo(), c.getNombre(),
                                subcursosSinEvaluaciones(alcance, c, evaluaciones)))
                        .toList(),
                filas);
    }

    // ── Escritura ──

    /** Solo notas nuevas: una ya registrada se cambia con corregir. */
    @Transactional
    public NotaFinalResponse.Detalle registrar(NotaFinalRequest request, Integer idUsuario, List<String> roles) {
        return guardar(request, false, idUsuario, roles);
    }

    /** Cambia o borra notas ya registradas (y también crea): pide el permiso de corregir. */
    @Transactional
    public NotaFinalResponse.Detalle corregir(NotaFinalRequest request, Integer idUsuario, List<String> roles) {
        return guardar(request, true, idUsuario, roles);
    }

    private NotaFinalResponse.Detalle guardar(NotaFinalRequest request, boolean correccion,
                                             Integer idUsuario, List<String> roles) {
        request.notas().stream().map(NotaFinalRequest.Item::idMatricula).distinct().sorted().forEach(bloqueo::matricula);
        Alcance alcance = alcance(request.idGradoSeccion(), request.idArea(), idUsuario, roles);
        GradoSeccion gs = alcance.gradoSeccion();
        if (gs.getAnioEscolar().getEstado() != AnioEscolarService.ESTADO_VIGENTE) {
            throw new IllegalArgumentException("Solo se registran notas en un año vigente");
        }
        PeriodoEvaluacion periodo = periodo(gs, request.periodo());
        String estado = periodo.estadoEn(LocalDate.now());
        if (PeriodoEvaluacion.POR_COMENZAR.equals(estado)) {
            throw new IllegalArgumentException("El periodo aún no empieza");
        }
        if (PeriodoEvaluacion.CERRADO.equals(estado)) {
            // Un código vale para todo el guardado, no para cada nota.
            notaService.consumirAutorizacion(request.codigoAutorizacion(), idUsuario);
        }

        EscalaCalificacion escala = configuracion(gs).getEscala();
        Map<Integer, Competencia> competencias = alcance.competencias().stream()
                .collect(Collectors.toMap(Competencia::getIdCompetencia, Function.identity()));
        Map<Integer, Matricula> alumnos = alumnosDe(gs).stream()
                .collect(Collectors.toMap(Matricula::getIdMatricula, Function.identity()));
        Map<String, Nota> existentes = finales(new ArrayList<>(alumnos.values()), competencias.keySet(), request.periodo());
        Usuario registrador = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("No se pudo identificar al usuario"));

        for (NotaFinalRequest.Item item : request.notas()) {
            Matricula matricula = alumnos.get(item.idMatricula());
            Competencia competencia = competencias.get(item.idCompetencia());
            if (matricula == null) {
                throw new IllegalArgumentException("Un alumno ya no está matriculado en esta sección");
            }
            if (competencia == null) {
                throw new IllegalArgumentException("No puedes calificar esa competencia en " + alcance.area().getNombre());
            }
            Nota actual = existentes.get(clave(item.idMatricula(), item.idCompetencia()));
            if (actual != null && !correccion) {
                throw new IllegalArgumentException("La competencia " + competencia.getCodigo() + " de "
                        + nombreAlumno(matricula) + " ya la registró " + nombre(actual.getUsuarioRegistro())
                        + ": cambiarla requiere el permiso de corregir notas");
            }
            if (item.calificacion() == null || item.calificacion().isBlank()) {
                if (actual != null) {
                    actual.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
                    notaRepository.save(actual);
                }
                continue;
            }
            String calificacion = Calificaciones.normalizar(escala, item.calificacion());
            String conclusion = Calificaciones.validarConclusion(escala, calificacion, item.conclusionDescriptiva(),
                    nombreAlumno(matricula) + " en la competencia " + competencia.getCodigo());
            Nota nota = actual != null ? actual : new Nota();
            nota.setMatricula(matricula);
            nota.setCompetencia(competencia);
            nota.setBimestre(periodo.getNumero());
            nota.setCalificacion(calificacion);
            nota.setConclusionDescriptiva(conclusion);
            nota.setFechaRegistro(LocalDate.now());
            nota.setUsuarioRegistro(registrador);
            nota.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
            notaRepository.save(nota);
        }
        return detalle(request.idGradoSeccion(), request.idArea(), request.periodo(), idUsuario, roles);
    }

    // ── Alcance ──

    /**
     * Docente: las áreas de sus asignaciones; si dicta un subcurso, solo las
     * competencias ligadas a él. Los demás: todas las áreas de cada sección.
     */
    private List<Alcance> alcances(Integer idAnio, Integer idUsuario, List<String> roles) {
        Acceso eliminado = eliminado();
        Optional<Docente> docente = accesoContextoService.esAdmin(roles)
                ? Optional.empty()
                : accesoContextoService.docenteDeUsuario(idUsuario);
        List<Alcance> lista = new ArrayList<>();
        if (docente.isPresent()) {
            Map<String, List<Asignacion>> porArea = asignacionRepository
                    .findByDocenteIdDocenteAndAccesoNot(docente.get().getIdDocente(), eliminado).stream()
                    .filter(a -> AccesoConstants.ACTIVO.equals(a.getAcceso().getIdAcceso()))
                    .filter(a -> a.getAnioEscolar().getIdAnio().equals(idAnio))
                    .collect(Collectors.groupingBy(a -> a.getGradoSeccion().getIdGradoSeccion() + "-" + area(a.getCurso()).getIdCurso(),
                            LinkedHashMap::new, Collectors.toList()));
            porArea.values().forEach(asignaciones -> {
                Set<Competencia> comp = new HashSet<>();
                asignaciones.forEach(a -> comp.addAll(competenciasDe(a.getCurso())));
                Asignacion primera = asignaciones.get(0);
                lista.add(new Alcance(primera.getGradoSeccion(), area(primera.getCurso()), ordenar(comp)));
            });
        } else {
            for (GradoSeccion gs : gradoSeccionRepository.findByAnioEscolarIdAnioAndAccesoNot(idAnio, eliminado)) {
                cursoRepository.findByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(idAnio,
                                gs.getGrado().getNivel().getIdNivel(), eliminado).stream()
                        .filter(c -> c.getTipo() != TipoCurso.SUBCURSO)
                        .filter(c -> AccesoConstants.ACTIVO.equals(c.getAcceso().getIdAcceso()))
                        .forEach(area -> lista.add(new Alcance(gs, area, competenciasDe(area))));
            }
        }
        return lista.stream()
                .filter(a -> !a.competencias().isEmpty())
                .sorted(Comparator.comparing((Alcance a) -> orden(a.gradoSeccion().getGrado().getNivel().getOrden()))
                        .thenComparing(a -> orden(a.gradoSeccion().getGrado().getOrden()))
                        .thenComparing(a -> MatriculaService.gradoYSeccion(a.gradoSeccion()))
                        .thenComparing(a -> a.area().getTipo() == TipoCurso.TRANSVERSAL)
                        .thenComparing(a -> a.area().getNombre()))
                .toList();
    }

    /** Si el usuario califica algo en esa sección (para exportar su registro). */
    @Transactional(readOnly = true)
    public boolean puedeVerSeccion(GradoSeccion gs, Integer idUsuario, List<String> roles) {
        return alcances(gs.getAnioEscolar().getIdAnio(), idUsuario, roles).stream()
                .anyMatch(a -> a.gradoSeccion().getIdGradoSeccion().equals(gs.getIdGradoSeccion()));
    }

    private Alcance alcance(Integer idGradoSeccion, Integer idArea, Integer idUsuario, List<String> roles) {
        GradoSeccion gs = gradoSeccionRepository.findById(idGradoSeccion)
                .orElseThrow(() -> new ResourceNotFoundException("Sección no encontrada"));
        return alcances(gs.getAnioEscolar().getIdAnio(), idUsuario, roles).stream()
                .filter(a -> a.gradoSeccion().getIdGradoSeccion().equals(idGradoSeccion)
                        && a.area().getIdCurso().equals(idArea))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Área no encontrada o fuera de tus asignaciones"));
    }

    private static Curso area(Curso curso) {
        return curso.getTipo() == TipoCurso.SUBCURSO && curso.getCursoPadre() != null ? curso.getCursoPadre() : curso;
    }

    /** Del área o transversal, todas; de un subcurso, las ligadas. Solo activas. */
    private List<Competencia> competenciasDe(Curso curso) {
        Collection<Competencia> todas = curso.getTipo() == TipoCurso.SUBCURSO
                ? curso.getCompetencias()
                : competenciaRepository.findByCursoIdCursoAndAccesoNot(curso.getIdCurso(), eliminado());
        return ordenar(todas.stream().filter(c -> AccesoConstants.ACTIVO.equals(c.getAcceso().getIdAcceso())).toList());
    }

    private static List<Competencia> ordenar(Collection<Competencia> competencias) {
        return competencias.stream().sorted(Comparator.comparing(Competencia::getOrden)).toList();
    }

    // ── Datos de apoyo ──

    /** Evaluaciones del periodo en esta sección hechas en el área o en cualquiera de sus subcursos. */
    private List<Evaluacion> evaluacionesDelArea(Alcance alcance, Integer periodo) {
        return evaluacionRepository.findByAsignacionGradoSeccionIdGradoSeccionAndPeriodoAndAccesoNot(
                        alcance.gradoSeccion().getIdGradoSeccion(), periodo.byteValue(), eliminado()).stream()
                .filter(e -> area(e.getAsignacion().getCurso()).getIdCurso().equals(alcance.area().getIdCurso()))
                .toList();
    }

    /** Subcursos asignados en la sección, ligados a la competencia, que aún no la evaluaron. */
    private List<String> subcursosSinEvaluaciones(Alcance alcance, Competencia competencia, List<Evaluacion> evaluaciones) {
        return asignacionRepository.findByAccesoNot(eliminado()).stream()
                .filter(a -> AccesoConstants.ACTIVO.equals(a.getAcceso().getIdAcceso()))
                .filter(a -> a.getGradoSeccion().getIdGradoSeccion().equals(alcance.gradoSeccion().getIdGradoSeccion()))
                .map(Asignacion::getCurso)
                .filter(c -> c.getTipo() == TipoCurso.SUBCURSO && c.getCursoPadre() != null
                        && c.getCursoPadre().getIdCurso().equals(alcance.area().getIdCurso()))
                .filter(c -> c.getCompetencias().stream().anyMatch(x -> x.getIdCompetencia().equals(competencia.getIdCompetencia())))
                .filter(c -> evaluaciones.stream().noneMatch(e -> e.getAsignacion().getCurso().getIdCurso().equals(c.getIdCurso())
                        && e.getCompetencias().stream().anyMatch(x -> x.getIdCompetencia().equals(competencia.getIdCompetencia()))))
                .map(Curso::getNombre)
                .distinct()
                .sorted()
                .toList();
    }

    private Map<String, Nota> finales(List<Matricula> alumnos, Set<Integer> competencias, Integer periodo) {
        if (alumnos.isEmpty() || competencias.isEmpty()) {
            return Map.of();
        }
        return notaRepository.findByMatriculaIdMatriculaInAndCompetenciaIdCompetenciaInAndBimestreAndAccesoNot(
                        alumnos.stream().map(Matricula::getIdMatricula).toList(), competencias, periodo.byteValue(), eliminado())
                .stream()
                .collect(Collectors.toMap(n -> clave(n.getMatricula().getIdMatricula(), n.getCompetencia().getIdCompetencia()),
                        Function.identity(), (a, b) -> a));
    }

    private PeriodoEvaluacion periodo(GradoSeccion gs, Integer numero) {
        return configuracionService.periodos(gs.getAnioEscolar(), gs.getGrado().getNivel()).stream()
                .filter(p -> numero != null && p.getNumero() == numero)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El periodo no existe: configúralo en Configuración académica"));
    }

    private List<Matricula> alumnosDe(GradoSeccion gs) {
        return matriculaRepository.findByGradoSeccionIdGradoSeccionAndAccesoNot(gs.getIdGradoSeccion(), eliminado()).stream()
                .filter(m -> AccesoConstants.ACTIVO.equals(m.getAcceso().getIdAcceso()))
                .sorted(MatriculaService.ORDEN_REPORTE)
                .toList();
    }

    private ConfiguracionAcademicaResponse configuracion(GradoSeccion gs) {
        return configuracionService.obtener(gs.getAnioEscolar().getIdAnio(), gs.getGrado().getNivel().getIdNivel());
    }

    private NotaFinalResponse.Curso aCurso(Alcance a) {
        GradoSeccion gs = a.gradoSeccion();
        ConfiguracionAcademicaResponse config = configuracion(gs);
        return new NotaFinalResponse.Curso(gs.getIdGradoSeccion(), a.area().getIdCurso(), a.area().getNombre(),
                a.area().getTipo() == TipoCurso.TRANSVERSAL, gs.getGrado().getNivel().getNombre(), gs.getGrado().getNombre(),
                gs.getSeccion() != null ? gs.getSeccion().getNombre() : null, gs.getTurno().getNombre(),
                gs.getAnioEscolar().getEstado() == AnioEscolarService.ESTADO_VIGENTE, config.getEscala(),
                config.getTipoPeriodo(), config.getPeriodos(),
                a.competencias().stream()
                        .map(c -> new EvaluacionResponse.Competencia(c.getIdCompetencia(), c.getCodigo(), c.getNombre()))
                        .toList());
    }

    private static Set<Integer> ids(List<Competencia> competencias) {
        return competencias.stream().map(Competencia::getIdCompetencia).collect(Collectors.toSet());
    }

    private static String clave(Integer idMatricula, Integer idCompetencia) {
        return idMatricula + "-" + idCompetencia;
    }

    private static String nombre(Usuario u) {
        return u == null ? null : u.getNombre() + " " + u.getApellidoPat();
    }

    private static String nombreAlumno(Matricula m) {
        Alumno a = m.getAlumnoApoderado().getAlumno();
        return a.getNombre() + " " + a.getApellidoPat();
    }

    private static int orden(Integer valor) {
        return valor == null ? Integer.MAX_VALUE : valor;
    }

    private Acceso eliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }
}
