package com.example.Escolar.Service;

import com.example.Escolar.Dto.ConfiguracionAcademicaResponse;
import com.example.Escolar.Dto.EvaluacionResponse;
import com.example.Escolar.Dto.NotaFinalRequest;
import com.example.Escolar.Dto.NotaFinalResponse;
import com.example.Escolar.Dto.ReaperturaNotaRequest;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final BloqueoCalificaciones bloqueo;
    private final HistorialMatriculaRepository historialRepository;
    private final ReconocimientoCalificacionRepository reconocimientoRepository;
    private final NotaFinalAnualRepository notaAnualRepository;

    /** Un área de una sección y las competencias que el usuario puede calificar en ella. */
    record Alcance(GradoSeccion gradoSeccion, Curso area, List<Competencia> competencias) {
    }

    /** Por alumno, sus secciones en el año; y sus notas de otras secciones aún no usadas aquí. */
    private record Traslados(Map<Integer, List<HistorialMatricula>> historial, List<NotaEvaluacion> notas) {
    }

    // ── Consultas ──

    @Transactional(readOnly = true)
    public List<NotaFinalResponse.Curso> cursos(Integer idAnio, Integer idUsuario, Integer idRolActivo) {
        return cursos(idAnio, idUsuario, idRolActivo, PERMISO);
    }

    List<NotaFinalResponse.Curso> cursos(Integer idAnio, Integer idUsuario, Integer idRolActivo, String permiso) {
        // La configuración es la misma para todo el nivel: se calcula una vez por nivel.
        Map<String, ConfiguracionAcademicaResponse> configs = new HashMap<>();
        return alcances(idAnio, idUsuario, idRolActivo, permiso, AccesoContextoService.Operacion.LECTURA).stream().map(a -> aCurso(a, configs)).toList();
    }

    @Transactional(readOnly = true)
    public NotaFinalResponse.Detalle detalle(Integer idGradoSeccion, Integer idArea, Integer periodo,
                                             Integer idUsuario, Integer idRolActivo) {
        Alcance alcance = alcance(idGradoSeccion, idArea, idUsuario, idRolActivo);
        ConfiguracionAcademicaResponse config = configuracion(alcance.gradoSeccion());
        PeriodoEvaluacion numero = periodo(alcance.gradoSeccion(), periodo);
        List<Matricula> alumnos = alumnosDe(alcance, idUsuario, idRolActivo, PERMISO, AccesoContextoService.Operacion.LECTURA);
        Set<Integer> competencias = ids(alcance.competencias());

        Map<String, Nota> finales = finales(alumnos, competencias, periodo);
        List<Evaluacion> evaluaciones = evaluacionesDelArea(alcance, periodo);
        Map<Integer, List<NotaEvaluacion>> notasPorEvaluacion = notaEvaluacionRepository
                .findByEvaluacionIdEvaluacionInAndAccesoNot(evaluaciones.stream().map(Evaluacion::getIdEvaluacion).toList(), eliminado()).stream()
                .collect(Collectors.groupingBy(n -> n.getEvaluacion().getIdEvaluacion()));

        Traslados traslados = traslados(alumnos, alcance, numero, evaluaciones);
        boolean cerrado = PeriodoEvaluacion.CERRADO.equals(numero.estadoEn(LocalDate.now()));
        Set<String> conAnual = conNotaAnual(alumnos, competencias);

        List<NotaFinalResponse.Alumno> filas = alumnos.stream().map(m -> {
            Alumno a = m.getAlumnoApoderado().getAlumno();
            List<HistorialMatricula> historial = traslados.historial().getOrDefault(m.getIdMatricula(), List.of());
            boolean cursoEnOtra = cursoEnOtraSeccion(historial, alcance.gradoSeccion(), numero);
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
                List<NotaFinalResponse.Referencia> referencias = traslados.notas().stream()
                        .filter(n -> n.getMatricula().getIdMatricula().equals(m.getIdMatricula()))
                        .filter(n -> n.getEvaluacion().getCompetencias().stream()
                                .anyMatch(x -> x.getIdCompetencia().equals(c.getIdCompetencia())))
                        .sorted(Comparator.comparing((NotaEvaluacion n) -> n.getEvaluacion().getFecha()))
                        .map(NotaFinalService::aReferencia)
                        .toList();
                return new NotaFinalResponse.Celda(c.getIdCompetencia(),
                        fin != null ? fin.getCalificacion() : null,
                        fin != null ? fin.getConclusionDescriptiva() : null,
                        fin != null ? nombre(fin.getUsuarioRegistro()) : null,
                        guia,
                        config.getEscala() == EscalaCalificacion.VIGESIMAL
                                ? Calificaciones.promedio(guia.stream().map(NotaFinalResponse.Guia::calificacion).toList())
                                : null,
                        fin != null ? seccionDistinta(historial, fin.getFechaRegistro(), alcance.gradoSeccion()) : null,
                        referencias,
                        fin == null && cerrado && cursoEnOtra,
                        conAnual.contains(clave(m.getIdMatricula(), c.getIdCompetencia())));
            }).toList();
            return new NotaFinalResponse.Alumno(m.getIdMatricula(), a.getCodigo(),
                    a.getApellidoPat() + " " + a.getApellidoMat() + ", " + a.getNombre(), celdas);
        }).toList();

        String estado = numero.estadoEn(LocalDate.now());
        boolean vigente = alcance.gradoSeccion().getAnioEscolar().getEstado() == AnioEscolarService.ESTADO_VIGENTE;
        boolean periodoEditable = vigente && !PeriodoEvaluacion.POR_COMENZAR.equals(estado)
                && puedeEditar(alcance, idUsuario, idRolActivo, PERMISO);
        return new NotaFinalResponse.Detalle(aCurso(alcance), config.getEscala(),
                new NotaFinalResponse.Periodo(numero.getNumero(), config.getTipoPeriodo().nombre(numero.getNumero()), estado),
                periodoEditable,
                periodoEditable && !PeriodoEvaluacion.CERRADO.equals(estado)
                        && accesoContextoService.tieneAccion(PERMISO, "CREAR", idUsuario, idRolActivo),
                periodoEditable && accesoContextoService.tieneAccion(PERMISO, "ACTUALIZAR", idUsuario, idRolActivo),
                alcance.competencias().stream()
                        .map(c -> new NotaFinalResponse.CompetenciaFinal(c.getIdCompetencia(), c.getCodigo(), c.getNombre(),
                                subcursosSinEvaluaciones(alcance, c, evaluaciones)))
                        .toList(),
                filas);
    }

    // ── Escritura ──

    /** Solo notas nuevas: una ya registrada se cambia con corregir. */
    @Transactional
    public NotaFinalResponse.Detalle registrar(NotaFinalRequest request, Integer idUsuario, Integer idRolActivo) {
        return guardar(request, false, idUsuario, idRolActivo);
    }

    /** Cambia o borra notas ya registradas (y también crea): pide el permiso de corregir. */
    @Transactional
    public NotaFinalResponse.Detalle corregir(NotaFinalRequest request, Integer idUsuario, Integer idRolActivo) {
        return guardar(request, true, idUsuario, idRolActivo);
    }

    private NotaFinalResponse.Detalle guardar(NotaFinalRequest request, boolean correccion,
                                             Integer idUsuario, Integer idRolActivo) {
        request.notas().stream().map(NotaFinalRequest.Item::idMatricula).distinct().sorted().forEach(bloqueo::matricula);
        Alcance alcance = alcance(request.idGradoSeccion(), request.idArea(), idUsuario, idRolActivo,
                PERMISO, AccesoContextoService.Operacion.ESCRITURA);
        GradoSeccion gs = alcance.gradoSeccion();
        if (gs.getAnioEscolar().getEstado() != AnioEscolarService.ESTADO_VIGENTE) {
            throw new IllegalArgumentException("Solo se registran notas en un año vigente");
        }
        PeriodoEvaluacion periodo = periodo(gs, request.periodo());
        String estado = periodo.estadoEn(LocalDate.now());
        if (PeriodoEvaluacion.POR_COMENZAR.equals(estado)) {
            throw new IllegalArgumentException("El periodo aún no empieza");
        }
        // Con el periodo cerrado solo escribe quien corrige (con motivo e historial al cambiar).
        if (PeriodoEvaluacion.CERRADO.equals(estado) && !correccion) {
            throw new IllegalArgumentException("El periodo ya cerró: sus notas las registra quien puede corregir notas");
        }

        EscalaCalificacion escala = configuracion(gs).getEscala();
        Map<Integer, Competencia> competencias = alcance.competencias().stream()
                .collect(Collectors.toMap(Competencia::getIdCompetencia, Function.identity()));
        Map<Integer, Matricula> alumnos = alumnosDe(alcance, idUsuario, idRolActivo, PERMISO, AccesoContextoService.Operacion.ESCRITURA).stream()
                .collect(Collectors.toMap(Matricula::getIdMatricula, Function.identity()));
        Map<String, Nota> existentes = finales(new ArrayList<>(alumnos.values()), competencias.keySet(), request.periodo());
        Map<Integer, List<HistorialMatricula>> historiales = PeriodoEvaluacion.CERRADO.equals(estado)
                ? historiales(new ArrayList<>(alumnos.values())) : Map.of();
        Usuario registrador = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("No se pudo identificar al usuario"));
        List<Evaluacion> evaluaciones = evaluacionesDelArea(alcance, request.periodo());
        Map<Integer, List<NotaEvaluacion>> notasPorEvaluacion = notasPorEvaluacion(evaluaciones);
        Set<String> conAnual = conNotaAnual(new ArrayList<>(alumnos.values()), competencias.keySet());
        String motivo = request.motivo() == null ? "" : request.motivo().trim();

        for (NotaFinalRequest.Item item : request.notas()) {
            Matricula matricula = alumnos.get(item.idMatricula());
            Competencia competencia = competencias.get(item.idCompetencia());
            if (matricula == null) {
                throw new IllegalArgumentException("Un alumno ya no está matriculado en esta sección");
            }
            if (competencia == null) {
                throw new IllegalArgumentException("No puedes calificar esa competencia en " + alcance.area().getNombre());
            }
            String quien = nombreAlumno(matricula) + " en la competencia " + competencia.getCodigo();
            Nota actual = existentes.get(clave(item.idMatricula(), item.idCompetencia()));
            if (actual != null && !correccion) {
                throw new IllegalArgumentException("La competencia " + competencia.getCodigo() + " de "
                        + nombreAlumno(matricula) + " ya la registró " + nombre(actual.getUsuarioRegistro())
                        + ": cambiarla requiere el permiso de corregir notas");
            }
            boolean cursoEnOtra = cursoEnOtraSeccion(historiales.getOrDefault(item.idMatricula(), List.of()), gs, periodo);
            if (actual == null && !correccion && cursoEnOtra) {
                throw new IllegalArgumentException(nombreAlumno(matricula) + " cursó el "
                        + configuracion(gs).getTipoPeriodo().nombre(periodo.getNumero()).toLowerCase()
                        + " en otra sección: su nota final la registra dirección");
            }
            if (actual != null && conAnual.contains(clave(item.idMatricula(), item.idCompetencia()))) {
                throw new IllegalArgumentException(quien + " ya tiene la nota final del año: reábrela antes");
            }
            List<String> guia = guia(evaluaciones, notasPorEvaluacion, item.idMatricula(), item.idCompetencia());
            // Dirección registra al que cursó el periodo cerrado en otra sección, guiada por esas notas.
            boolean excepcionTraslado = correccion && actual == null && cursoEnOtra;
            if (actual == null && guia.isEmpty() && !excepcionTraslado) {
                throw new IllegalArgumentException(quien + " no tiene notas de evaluaciones: califícalo primero");
            }
            if (escala != EscalaCalificacion.VIGESIMAL && (item.calificacion() == null || item.calificacion().isBlank())) {
                throw new IllegalArgumentException("Falta la nota de " + quien + ": para quitar una nota, reábrela");
            }
            String calificacion = calificacionFinal(escala, actual, guia, item.calificacion());
            String conclusion = Calificaciones.validarConclusion(escala, calificacion, item.conclusionDescriptiva(), quien);
            if (actual != null) {
                if (calificacion.equals(actual.getCalificacion())
                        && Objects.equals(conclusion, actual.getConclusionDescriptiva())) {
                    continue;
                }
                if (motivo.isEmpty()) {
                    throw new IllegalArgumentException("Escribe el motivo de la corrección");
                }
                anular(actual, motivo, registrador);
            }
            Nota nota = new Nota();
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
        return detalle(request.idGradoSeccion(), request.idArea(), request.periodo(), idUsuario, idRolActivo);
    }

    /**
     * Anula la nota final con su motivo: las evaluaciones del alumno en esa
     * competencia se desbloquean y la nota se vuelve a registrar. No se puede si
     * ya está la nota del año.
     */
    @Transactional
    public NotaFinalResponse.Detalle reabrir(ReaperturaNotaRequest request, Integer idUsuario, Integer idRolActivo) {
        if (request.periodo() == null) {
            throw new IllegalArgumentException("Elige el periodo");
        }
        bloqueo.matricula(request.idMatricula());
        Alcance alcance = alcance(request.idGradoSeccion(), request.idArea(), idUsuario, idRolActivo,
                PERMISO, AccesoContextoService.Operacion.ESCRITURA);
        GradoSeccion gs = alcance.gradoSeccion();
        if (gs.getAnioEscolar().getEstado() != AnioEscolarService.ESTADO_VIGENTE) {
            throw new IllegalArgumentException("Solo se reabren notas en un año vigente");
        }
        PeriodoEvaluacion periodo = periodo(gs, request.periodo());
        Matricula matricula = alumnosDe(alcance, idUsuario, idRolActivo, PERMISO, AccesoContextoService.Operacion.ESCRITURA).stream()
                .filter(m -> m.getIdMatricula().equals(request.idMatricula()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El alumno ya no está matriculado en esta sección"));
        Competencia competencia = alcance.competencias().stream()
                .filter(c -> c.getIdCompetencia().equals(request.idCompetencia()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No puedes calificar esa competencia en " + alcance.area().getNombre()));
        Nota actual = finales(List.of(matricula), Set.of(competencia.getIdCompetencia()), request.periodo())
                .get(clave(matricula.getIdMatricula(), competencia.getIdCompetencia()));
        if (actual == null) {
            throw new IllegalArgumentException("No hay nota final que reabrir");
        }
        if (!conNotaAnual(List.of(matricula), Set.of(competencia.getIdCompetencia())).isEmpty()) {
            throw new IllegalArgumentException(nombreAlumno(matricula) + " ya tiene la nota final del año en la competencia "
                    + competencia.getCodigo() + ": reábrela antes");
        }
        anular(actual, request.motivo().trim(), usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("No se pudo identificar al usuario")));
        return detalle(request.idGradoSeccion(), request.idArea(), request.periodo(), idUsuario, idRolActivo);
    }

    /** Vigesimal: el promedio de las evaluaciones (o la ya registrada, que solo se reabre). Literal: la elegida. */
    private static String calificacionFinal(EscalaCalificacion escala, Nota actual, List<String> guia, String elegida) {
        if (escala == EscalaCalificacion.VIGESIMAL) {
            if (actual != null) {
                return actual.getCalificacion();
            }
            Integer promedio = Calificaciones.promedio(guia);
            if (promedio != null) {
                return String.valueOf(promedio);
            }
        }
        return Calificaciones.normalizar(escala, elegida);
    }

    private void anular(Nota nota, String motivo, Usuario usuario) {
        nota.setAcceso(eliminado());
        nota.setMotivoAnulacion(motivo);
        nota.setUsuarioAnulacion(usuario);
        nota.setFechaAnulacion(LocalDateTime.now());
        notaRepository.save(nota);
    }

    /** Notas del alumno en las evaluaciones de esta sección que afectan la competencia. */
    private static List<String> guia(List<Evaluacion> evaluaciones, Map<Integer, List<NotaEvaluacion>> notasPorEvaluacion,
                                     Integer idMatricula, Integer idCompetencia) {
        return evaluaciones.stream()
                .filter(e -> e.getCompetencias().stream().anyMatch(x -> x.getIdCompetencia().equals(idCompetencia)))
                .flatMap(e -> notasPorEvaluacion.getOrDefault(e.getIdEvaluacion(), List.of()).stream())
                .filter(n -> n.getMatricula().getIdMatricula().equals(idMatricula))
                .map(NotaEvaluacion::getCalificacion)
                .toList();
    }

    private Map<Integer, List<NotaEvaluacion>> notasPorEvaluacion(List<Evaluacion> evaluaciones) {
        if (evaluaciones.isEmpty()) {
            return Map.of();
        }
        return notaEvaluacionRepository.findByEvaluacionIdEvaluacionInAndAccesoNot(
                        evaluaciones.stream().map(Evaluacion::getIdEvaluacion).toList(), eliminado()).stream()
                .collect(Collectors.groupingBy(n -> n.getEvaluacion().getIdEvaluacion()));
    }

    /** Claves alumno-competencia que ya tienen la nota del año. */
    private Set<String> conNotaAnual(List<Matricula> alumnos, Set<Integer> competencias) {
        if (alumnos.isEmpty() || competencias.isEmpty()) {
            return Set.of();
        }
        return notaAnualRepository.findByMatriculaIdMatriculaInAndCompetenciaIdCompetenciaInAndAccesoNot(
                        alumnos.stream().map(Matricula::getIdMatricula).toList(), competencias, eliminado()).stream()
                .map(n -> clave(n.getMatricula().getIdMatricula(), n.getCompetencia().getIdCompetencia()))
                .collect(Collectors.toSet());
    }

    // ── Alcance ──

    /**
     * Docente: las áreas de sus asignaciones; si dicta un subcurso, solo las
     * competencias ligadas a él. Los demás: todas las áreas de cada sección.
     */
    private List<Alcance> alcances(Integer idAnio, Integer idUsuario, Integer idRolActivo) {
        return alcances(idAnio, idUsuario, idRolActivo, PERMISO, AccesoContextoService.Operacion.LECTURA);
    }

    private List<Alcance> alcances(Integer idAnio, Integer idUsuario, Integer idRolActivo,
                                  String permiso, AccesoContextoService.Operacion operacion) {
        List<Alcance> lista = switch (accesoContextoService.alcance(permiso, operacion, idUsuario, idRolActivo)) {
            case TODO -> alcancesGlobales(idAnio);
            case DOCENTE -> alcancesDeDocente(idAnio, idUsuario);
            case APODERADO -> alcancesDeApoderado(idAnio, idUsuario);
            case NINGUNO -> List.of();
        };
        return lista.stream()
                .filter(a -> !a.competencias().isEmpty())
                .sorted(Comparator.comparing((Alcance a) -> orden(a.gradoSeccion().getGrado().getNivel().getOrden()))
                        .thenComparing(a -> orden(a.gradoSeccion().getGrado().getOrden()))
                        .thenComparing(a -> MatriculaService.gradoYSeccion(a.gradoSeccion()))
                        .thenComparing(a -> a.area().getTipo() == TipoCurso.TRANSVERSAL)
                        .thenComparing(a -> a.area().getNombre()))
                .toList();
    }

    private List<Alcance> alcancesDeDocente(Integer idAnio, Integer idUsuario) {
        Acceso eliminado = eliminado();
        return accesoContextoService.docenteDeUsuario(idUsuario)
                .map(docente -> asignacionRepository
                        .findByDocenteIdDocenteAndAccesoNot(docente.getIdDocente(), eliminado).stream()
                        .filter(a -> AccesoConstants.ACTIVO.equals(a.getAcceso().getIdAcceso()))
                        .filter(a -> a.getAnioEscolar().getIdAnio().equals(idAnio))
                        .collect(Collectors.groupingBy(
                                a -> a.getGradoSeccion().getIdGradoSeccion() + "-" + area(a.getCurso()).getIdCurso(),
                                LinkedHashMap::new, Collectors.toList())))
                .map(porArea -> {
                    List<Alcance> lista = new ArrayList<>();
                    Map<Integer, List<Competencia>> competenciasPorCurso = new HashMap<>();
                    porArea.values().forEach(asignaciones -> {
                        Set<Competencia> comp = new HashSet<>();
                        asignaciones.forEach(a -> comp.addAll(competenciasPorCurso.computeIfAbsent(
                                a.getCurso().getIdCurso(), k -> competenciasDe(a.getCurso()))));
                        Asignacion primera = asignaciones.get(0);
                        lista.add(new Alcance(primera.getGradoSeccion(), area(primera.getCurso()), ordenar(comp)));
                    });
                    return lista;
                })
                .orElseGet(List::of);
    }

    private List<Alcance> alcancesGlobales(Integer idAnio) {
        Acceso eliminado = eliminado();
        List<Alcance> lista = new ArrayList<>();
        Map<Integer, List<Curso>> areasPorNivel = new HashMap<>();
        Map<Integer, List<Competencia>> competenciasPorArea = new HashMap<>();
        for (GradoSeccion gs : gradoSeccionRepository.findByAnioEscolarIdAnioAndAccesoNot(idAnio, eliminado)) {
            areasPorNivel.computeIfAbsent(gs.getGrado().getNivel().getIdNivel(), idNivel ->
                            cursoRepository.findByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(idAnio, idNivel, eliminado).stream()
                                    .filter(c -> c.getTipo() != TipoCurso.SUBCURSO)
                                    .filter(c -> AccesoConstants.ACTIVO.equals(c.getAcceso().getIdAcceso()))
                                    .toList())
                    .forEach(area -> lista.add(new Alcance(gs, area,
                            competenciasPorArea.computeIfAbsent(area.getIdCurso(), k -> competenciasDe(area)))));
        }
        return lista;
    }

    private List<Alcance> alcancesDeApoderado(Integer idAnio, Integer idUsuario) {
        List<Alcance> lista = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        Map<Integer, List<Curso>> areasPorNivel = new HashMap<>();
        Map<Integer, List<Competencia>> competenciasPorArea = new HashMap<>();
        Optional<Apoderado> apoderado = accesoContextoService.apoderadoDeUsuario(idUsuario);
        if (apoderado.isEmpty()) {
            return lista;
        }
        for (Matricula matricula : accesoContextoService.matriculasDeHijos(apoderado.get())) {
            GradoSeccion gs = matricula.getGradoSeccion();
            if (!gs.getAnioEscolar().getIdAnio().equals(idAnio)
                    || !AccesoConstants.ACTIVO.equals(matricula.getAcceso().getIdAcceso())) {
                continue;
            }
            areasPorNivel.computeIfAbsent(gs.getGrado().getNivel().getIdNivel(), idNivel ->
                            cursoRepository.findByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(
                                            idAnio, idNivel, eliminado()).stream()
                                    .filter(c -> c.getTipo() != TipoCurso.SUBCURSO)
                                    .filter(c -> AccesoConstants.ACTIVO.equals(c.getAcceso().getIdAcceso()))
                                    .toList())
                    .forEach(area -> {
                        String clave = gs.getIdGradoSeccion() + "-" + area.getIdCurso();
                        if (vistos.add(clave)) {
                            lista.add(new Alcance(gs, area,
                                    competenciasPorArea.computeIfAbsent(area.getIdCurso(), k -> competenciasDe(area))));
                        }
                    });
        }
        return lista;
    }


    /** Las exportaciones de la seccion completa incluyen todas sus areas y alumnos. */
    public boolean puedeExportarSeccionCompleta(Integer idUsuario, Integer idRolActivo) {
        return accesoContextoService.alcance(PERMISO, AccesoContextoService.Operacion.EXPORTACION, idUsuario, idRolActivo)
                == AccesoContextoService.Alcance.TODO;
    }

    Alcance alcance(Integer idGradoSeccion, Integer idArea, Integer idUsuario, Integer idRolActivo) {
        return alcance(idGradoSeccion, idArea, idUsuario, idRolActivo, PERMISO, AccesoContextoService.Operacion.LECTURA);
    }

    Alcance alcance(Integer idGradoSeccion, Integer idArea, Integer idUsuario, Integer idRolActivo,
                    String permiso, AccesoContextoService.Operacion operacion) {
        GradoSeccion gs = gradoSeccionRepository.findById(idGradoSeccion)
                .orElseThrow(() -> new ResourceNotFoundException("Sección no encontrada"));
        return alcances(gs.getAnioEscolar().getIdAnio(), idUsuario, idRolActivo, permiso, operacion).stream()
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

    // ── Alumnos trasladados ──

    /**
     * Áreas y periodos cerrados en que al alumno le falta alguna nota final. Se
     * avisa antes de cambiarlo de sección: luego solo dirección podrá registrarlas.
     */
    @Transactional(readOnly = true)
    public List<String> pendientesTraslado(Integer idMatricula) {
        Matricula m = matriculaRepository.findById(idMatricula)
                .orElseThrow(() -> new ResourceNotFoundException("Matrícula no encontrada"));
        GradoSeccion gs = m.getGradoSeccion();
        if (gs.getAnioEscolar().getEstado() != AnioEscolarService.ESTADO_VIGENTE) {
            return List.of();
        }
        ConfiguracionAcademicaResponse config = configuracion(gs);
        List<PeriodoEvaluacion> cerrados = configuracionService.periodos(gs.getAnioEscolar(), gs.getGrado().getNivel()).stream()
                .filter(p -> PeriodoEvaluacion.CERRADO.equals(p.estadoEn(LocalDate.now())))
                .sorted(Comparator.comparing(PeriodoEvaluacion::getNumero))
                .toList();
        if (cerrados.isEmpty()) {
            return List.of();
        }
        Set<String> conNota = notaRepository.findByMatriculaIdMatriculaAndAccesoNot(idMatricula, eliminado()).stream()
                .map(n -> n.getBimestre() + "-" + n.getCompetencia().getIdCompetencia())
                .collect(Collectors.toSet());
        List<String> pendientes = new ArrayList<>();
        cursoRepository.findByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(gs.getAnioEscolar().getIdAnio(),
                        gs.getGrado().getNivel().getIdNivel(), eliminado()).stream()
                .filter(c -> c.getTipo() != TipoCurso.SUBCURSO && AccesoConstants.ACTIVO.equals(c.getAcceso().getIdAcceso()))
                .sorted(Comparator.comparing((Curso c) -> c.getTipo() == TipoCurso.TRANSVERSAL).thenComparing(Curso::getNombre))
                .forEach(area -> {
                    List<Competencia> competencias = competenciasDe(area);
                    List<String> periodos = cerrados.stream()
                            .filter(p -> competencias.stream().anyMatch(c -> !conNota.contains(p.getNumero() + "-" + c.getIdCompetencia())))
                            .map(p -> config.getTipoPeriodo().nombre(p.getNumero()))
                            .toList();
                    if (!competencias.isEmpty() && !periodos.isEmpty()) {
                        pendientes.add(area.getNombre() + ": " + String.join(", ", periodos));
                    }
                });
        return pendientes;
    }

    private Map<Integer, List<HistorialMatricula>> historiales(List<Matricula> alumnos) {
        if (alumnos.isEmpty()) {
            return Map.of();
        }
        return historialRepository.findByMatriculaIdMatriculaInAndAccesoNot(
                        alumnos.stream().map(Matricula::getIdMatricula).toList(), eliminado()).stream()
                .collect(Collectors.groupingBy(h -> h.getMatricula().getIdMatricula()));
    }

    /**
     * Notas del periodo en el área puestas en otras secciones. Fuera: las ya
     * usadas en esta sección y las copias (se ve la original).
     */
    private Traslados traslados(List<Matricula> alumnos, Alcance alcance, PeriodoEvaluacion periodo,
                                List<Evaluacion> evaluaciones) {
        if (alumnos.isEmpty()) {
            return new Traslados(Map.of(), List.of());
        }
        List<Integer> ids = alumnos.stream().map(Matricula::getIdMatricula).toList();
        Set<Integer> deEstaSeccion = evaluaciones.stream().map(Evaluacion::getIdEvaluacion).collect(Collectors.toSet());
        Set<Integer> excluidas = new HashSet<>();
        reconocimientoRepository.movimientosDe(ids).stream()
                .filter(r -> Byte.valueOf(ReconocimientoCalificacion.VIGENTE).equals(r.getVigencia()))
                .forEach(r -> {
                    excluidas.add(r.getDestino());
                    if (deEstaSeccion.contains(r.getEvaluacion())) {
                        excluidas.add(r.getOrigen());
                    }
                });
        Integer seccion = alcance.gradoSeccion().getIdGradoSeccion();
        Integer anio = alcance.gradoSeccion().getAnioEscolar().getIdAnio();
        List<NotaEvaluacion> notas = notaEvaluacionRepository
                .findByMatriculaIdMatriculaInAndEvaluacionPeriodoAndAccesoNot(ids, periodo.getNumero(), eliminado()).stream()
                .filter(n -> !excluidas.contains(n.getIdNotaEvaluacion()))
                .filter(n -> {
                    Asignacion a = n.getEvaluacion().getAsignacion();
                    return !a.getGradoSeccion().getIdGradoSeccion().equals(seccion)
                            && a.getAnioEscolar().getIdAnio().equals(anio)
                            && AccesoConstants.ACTIVO.equals(n.getEvaluacion().getAcceso().getIdAcceso())
                            && area(a.getCurso()).getIdCurso().equals(alcance.area().getIdCurso());
                })
                .toList();
        return new Traslados(historiales(alumnos), notas);
    }

    /** Llegó a la sección después de terminado el periodo: lo cursó en otra. */
    static boolean cursoEnOtraSeccion(List<HistorialMatricula> historial, GradoSeccion actual, PeriodoEvaluacion periodo) {
        return historial.stream()
                .filter(h -> h.getFechaFinal() == null && h.getGradoSeccion().getIdGradoSeccion().equals(actual.getIdGradoSeccion()))
                .map(HistorialMatricula::getFechaInicio)
                .filter(Objects::nonNull)
                .anyMatch(llegada -> llegada.isAfter(periodo.getFechaFin()));
    }

    /** Sección del alumno en esa fecha, si no es la actual: "3ro B · Mañana". */
    static String seccionDistinta(List<HistorialMatricula> historial, LocalDate fecha, GradoSeccion actual) {
        if (fecha == null) {
            return null;
        }
        return historial.stream()
                .filter(h -> h.getFechaInicio() != null && !fecha.isBefore(h.getFechaInicio())
                        && (h.getFechaFinal() == null || fecha.isBefore(h.getFechaFinal())))
                .map(HistorialMatricula::getGradoSeccion)
                .filter(gs -> !gs.getIdGradoSeccion().equals(actual.getIdGradoSeccion()))
                .map(ReconocimientoCalificacionService::nombreSeccion)
                .findFirst()
                .orElse(null);
    }

    private static NotaFinalResponse.Referencia aReferencia(NotaEvaluacion n) {
        Evaluacion e = n.getEvaluacion();
        return new NotaFinalResponse.Referencia(e.getNombre(), e.getAsignacion().getCurso().getNombre(),
                ReconocimientoCalificacionService.nombreSeccion(e.getAsignacion().getGradoSeccion()), e.getFecha(),
                n.getCalificacion());
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

    boolean puedeEditar(Alcance curso, Integer idUsuario, Integer idRolActivo, String permiso) {
        return seccionCompleta(curso, idUsuario, accesoContextoService.alcance(
                permiso, AccesoContextoService.Operacion.ESCRITURA, idUsuario, idRolActivo));
    }

    private boolean seccionCompleta(Alcance curso, Integer idUsuario, AccesoContextoService.Alcance alcance) {
        return alcance == AccesoContextoService.Alcance.TODO
                || alcance != null && alcance.incluyeDocente()
                        && accesoContextoService.asignacionesActivasDeDocente(idUsuario).stream()
                                .anyMatch(a -> a.getGradoSeccion().getIdGradoSeccion()
                                        .equals(curso.gradoSeccion().getIdGradoSeccion())
                                        && area(a.getCurso()).getIdCurso().equals(curso.area().getIdCurso()));
    }

    List<Matricula> alumnosDe(Alcance curso, Integer idUsuario, Integer idRolActivo,
                              String permiso, AccesoContextoService.Operacion operacion) {
        GradoSeccion gs = curso.gradoSeccion();
        AccesoContextoService.Alcance alcance = accesoContextoService.alcance(permiso, operacion, idUsuario, idRolActivo);
        boolean seccionCompleta = seccionCompleta(curso, idUsuario, alcance);
        Set<Integer> hijos = !seccionCompleta && alcance != null && alcance.incluyeApoderado()
                ? accesoContextoService.apoderadoDeUsuario(idUsuario)
                        .map(a -> accesoContextoService.matriculasDeHijos(a).stream()
                                .map(Matricula::getIdMatricula).collect(Collectors.toSet()))
                        .orElseGet(Set::of) : Set.of();
        return matriculaRepository.findByGradoSeccionIdGradoSeccionAndAccesoNot(gs.getIdGradoSeccion(), eliminado()).stream()
                .filter(m -> AccesoConstants.ACTIVO.equals(m.getAcceso().getIdAcceso()))
                .filter(m -> seccionCompleta || hijos.contains(m.getIdMatricula()))
                .sorted(MatriculaService.ORDEN_REPORTE)
                .toList();
    }

    ConfiguracionAcademicaResponse configuracion(GradoSeccion gs) {
        return configuracionService.obtener(gs.getAnioEscolar().getIdAnio(), gs.getGrado().getNivel().getIdNivel());
    }

    NotaFinalResponse.Curso aCurso(Alcance a) {
        return aCurso(a, new HashMap<>());
    }

    private NotaFinalResponse.Curso aCurso(Alcance a, Map<String, ConfiguracionAcademicaResponse> configs) {
        GradoSeccion gs = a.gradoSeccion();
        ConfiguracionAcademicaResponse config = configs.computeIfAbsent(
                gs.getAnioEscolar().getIdAnio() + "-" + gs.getGrado().getNivel().getIdNivel(), k -> configuracion(gs));
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

    static String clave(Integer idMatricula, Integer idCompetencia) {
        return idMatricula + "-" + idCompetencia;
    }

    static String nombre(Usuario u) {
        return u == null ? null : u.getNombre() + " " + u.getApellidoPat();
    }

    static String nombreAlumno(Matricula m) {
        Alumno a = m.getAlumnoApoderado().getAlumno();
        return a.getNombre() + " " + a.getApellidoPat();
    }

    private static int orden(Integer valor) {
        return valor == null ? Integer.MAX_VALUE : valor;
    }

    Acceso eliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }
}
