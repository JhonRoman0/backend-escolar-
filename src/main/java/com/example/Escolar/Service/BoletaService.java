package com.example.Escolar.Service;

import com.example.Escolar.Dto.BoletaResponse;
import com.example.Escolar.Dto.ConfiguracionAcademicaResponse;
import com.example.Escolar.Dto.EvaluacionResponse;
import com.example.Escolar.Dto.PeriodoEvaluacionResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Datos de la boleta de notas de una sección en un periodo: las notas finales
 * de cada competencia en todos los periodos hasta el elegido, las conclusiones
 * del periodo y el resumen de asistencia. El PDF se arma en el frontend.
 */
@Service
@RequiredArgsConstructor
public class BoletaService {

    private final NotaRepository notaRepository;
    private final MatriculaRepository matriculaRepository;
    private final GradoSeccionRepository gradoSeccionRepository;
    private final AsistenciaAlumnoRepository asistenciaRepository;
    private final AccesoRepository accesoRepository;
    private final ConfiguracionAcademicaService configuracionService;
    private final NotaFinalService notaFinalService;
    private final MallaEvaluable mallaEvaluable;

    @Transactional(readOnly = true)
    public BoletaResponse boletas(Integer idGradoSeccion, Integer periodo, Integer idUsuario, List<String> roles) {
        Acceso eliminado = accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
        GradoSeccion gs = gradoSeccionRepository.findById(idGradoSeccion)
                .filter(g -> !AccesoConstants.ELIMINADO.equals(g.getAcceso().getIdAcceso()))
                .orElseThrow(() -> new ResourceNotFoundException("Sección no encontrada"));
        if (!notaFinalService.puedeVerSeccion(gs, idUsuario, roles)) {
            throw new ResourceNotFoundException("Sección no encontrada o fuera de tus asignaciones");
        }
        AnioEscolar anio = gs.getAnioEscolar();
        Nivel nivel = gs.getGrado().getNivel();
        ConfiguracionAcademicaResponse config = configuracionService.obtener(anio.getIdAnio(), nivel.getIdNivel());
        PeriodoEvaluacion actual = configuracionService.periodos(anio, nivel).stream()
                .filter(p -> periodo != null && p.getNumero() == periodo)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El periodo no existe: configúralo en Configuración académica"));
        List<PeriodoEvaluacionResponse> hastaElActual = config.getPeriodos().stream()
                .filter(p -> p.numero() <= periodo)
                .toList();

        List<MallaEvaluable.Area> areas = mallaEvaluable.areas(anio, nivel);
        List<Matricula> alumnos = matriculaRepository.findByGradoSeccionIdGradoSeccionAndAccesoNot(idGradoSeccion, eliminado).stream()
                .filter(m -> AccesoConstants.ACTIVO.equals(m.getAcceso().getIdAcceso()))
                .sorted(MatriculaService.ORDEN_REPORTE)
                .toList();

        // matrícula → competencia → periodo → nota
        Map<Integer, Map<Integer, Map<Integer, Nota>>> notas = new HashMap<>();
        for (PeriodoEvaluacionResponse p : hastaElActual) {
            notaRepository.findByMatriculaGradoSeccionIdGradoSeccionAndBimestreAndAccesoNot(idGradoSeccion, (byte) p.numero(), eliminado)
                    .forEach(n -> notas.computeIfAbsent(n.getMatricula().getIdMatricula(), k -> new HashMap<>())
                            .computeIfAbsent(n.getCompetencia().getIdCompetencia(), k -> new HashMap<>())
                            .put(p.numero(), n));
        }

        // Solo los días ya transcurridos del periodo.
        LocalDate hasta = actual.getFechaFin().isBefore(LocalDate.now()) ? actual.getFechaFin() : LocalDate.now();
        Set<Integer> ids = alumnos.stream().map(Matricula::getIdMatricula).collect(Collectors.toSet());
        Map<Integer, List<AsistenciaAlumno>> asistencias = asistenciaRepository
                .findByFechaBetweenAndAccesoNot(actual.getFechaInicio(), hasta, eliminado).stream()
                .filter(a -> ids.contains(a.getMatricula().getIdMatricula()))
                .collect(Collectors.groupingBy(a -> a.getMatricula().getIdMatricula()));

        List<BoletaResponse.AlumnoBoleta> filas = alumnos.stream().map(m -> {
            Alumno a = m.getAlumnoApoderado().getAlumno();
            Map<Integer, Map<Integer, Nota>> suyas = notas.getOrDefault(m.getIdMatricula(), Map.of());
            Map<Integer, Map<Integer, String>> calificaciones = new HashMap<>();
            Map<Integer, String> conclusiones = new HashMap<>();
            suyas.forEach((idCompetencia, porPeriodo) -> {
                Map<Integer, String> valores = new HashMap<>();
                porPeriodo.forEach((p, n) -> valores.put(p, n.getCalificacion()));
                calificaciones.put(idCompetencia, valores);
                Nota delActual = porPeriodo.get(periodo);
                if (delActual != null && delActual.getConclusionDescriptiva() != null) {
                    conclusiones.put(idCompetencia, delActual.getConclusionDescriptiva());
                }
            });
            return new BoletaResponse.AlumnoBoleta(m.getIdMatricula(), a.getCodigo(), a.getDocumentoIdentidad(),
                    a.getApellidoPat() + " " + a.getApellidoMat() + ", " + a.getNombre(), calificaciones, conclusiones,
                    resumen(asistencias.getOrDefault(m.getIdMatricula(), List.of())));
        }).toList();

        return new BoletaResponse(
                new BoletaResponse.Seccion(anio.getAnio(), nivel.getNombre(), gs.getGrado().getNombre(),
                        gs.getSeccion() != null ? gs.getSeccion().getNombre() : null, gs.getTurno().getNombre()),
                config.getEscala(), config.getTipoPeriodo(), hastaElActual, periodo,
                areas.stream().map(area -> new BoletaResponse.Area(area.curso().getNombre(),
                        area.curso().getTipo() == TipoCurso.TRANSVERSAL,
                        area.competencias().stream()
                                .map(c -> new EvaluacionResponse.Competencia(c.getIdCompetencia(), c.getCodigo(), c.getNombre()))
                                .toList())).toList(),
                filas);
    }

    private static BoletaResponse.Asistencia resumen(List<AsistenciaAlumno> dias) {
        Map<String, Long> porEstado = dias.stream()
                .collect(Collectors.groupingBy(d -> clave(d.getEstado().getNombre()), Collectors.counting()));
        return new BoletaResponse.Asistencia(
                porEstado.getOrDefault("PUNTUAL", 0L).intValue(),
                porEstado.getOrDefault("TARDANZA", 0L).intValue(),
                porEstado.getOrDefault("JUSTIFICADA", 0L).intValue(),
                porEstado.getOrDefault("INASISTENCIA", 0L).intValue());
    }

    private static String clave(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").trim().toUpperCase();
    }
}
