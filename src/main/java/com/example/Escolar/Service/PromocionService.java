package com.example.Escolar.Service;

import com.example.Escolar.Dto.ConfiguracionAcademicaResponse;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.NotaFinalAnualRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Si el alumno pasa de grado o repite, con sus notas finales del año. Un área
 * está desaprobada si en vigesimal el promedio de sus competencias queda bajo la
 * nota mínima, o si en literal la mitad o más de ellas están bajo la mínima.
 * Repite con tantas áreas desaprobadas como diga la configuración del nivel.
 * Las competencias transversales no cuentan.
 */
@Service
@RequiredArgsConstructor
public class PromocionService {

    public enum Situacion {
        PROMOVIDO,
        REPITE,
        /** Le falta alguna nota del año: no se decide todavía. */
        FALTAN_NOTAS,
        /** La sección no usó notas del año (años anteriores a ellas): decide quien matricula. */
        SIN_NOTAS_DEL_ANIO
    }

    /** desaprobadas: nombres de las áreas. faltantes: "Matemática C2". */
    public record Resultado(Situacion situacion, List<String> desaprobadas, List<String> faltantes) {
    }

    private final MallaEvaluable mallaEvaluable;
    private final NotaFinalAnualRepository anualRepository;
    private final ConfiguracionAcademicaService configuracionService;
    private final AccesoRepository accesoRepository;

    /** La situación con la que terminó la matrícula de un año ya cerrado. */
    public Resultado situacion(Matricula matricula) {
        Acceso eliminado = accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
        GradoSeccion gs = matricula.getGradoSeccion();
        if (!anualRepository.existsByMatriculaGradoSeccionIdGradoSeccionAndAccesoNot(gs.getIdGradoSeccion(), eliminado)) {
            return new Resultado(Situacion.SIN_NOTAS_DEL_ANIO, List.of(), List.of());
        }
        AnioEscolar anio = gs.getAnioEscolar();
        Nivel nivel = gs.getGrado().getNivel();
        ConfiguracionAcademicaResponse config = configuracionService.obtener(anio.getIdAnio(), nivel.getIdNivel());
        Map<Integer, String> notas = anualRepository.findByMatriculaIdMatriculaAndAccesoNot(matricula.getIdMatricula(), eliminado)
                .stream()
                .collect(Collectors.toMap(n -> n.getCompetencia().getIdCompetencia(), NotaFinalAnual::getCalificacion, (a, b) -> a));

        List<String> faltantes = new ArrayList<>();
        List<String> desaprobadas = new ArrayList<>();
        for (MallaEvaluable.Area area : mallaEvaluable.areas(anio, nivel)) {
            if (area.curso().getTipo() == TipoCurso.TRANSVERSAL) {
                continue;
            }
            List<String> delArea = new ArrayList<>();
            for (Competencia c : area.competencias()) {
                String nota = notas.get(c.getIdCompetencia());
                if (nota == null) {
                    faltantes.add(area.curso().getNombre() + " C" + c.getCodigo());
                } else {
                    delArea.add(nota);
                }
            }
            if (delArea.size() == area.competencias().size()
                    && areaDesaprobada(config.getEscala(), delArea, config.getNotaMinima())) {
                desaprobadas.add(area.curso().getNombre());
            }
        }
        if (!faltantes.isEmpty()) {
            return new Resultado(Situacion.FALTAN_NOTAS, desaprobadas, faltantes);
        }
        // Recuperación (pendiente): un alumno que repite podría aprobar sus áreas en
        // la recuperación de verano; ese resultado se aplicaría aquí antes de decidir.
        Situacion situacion = desaprobadas.size() >= config.getAreasParaRepetir() ? Situacion.REPITE : Situacion.PROMOVIDO;
        return new Resultado(situacion, desaprobadas, List.of());
    }

    /** Vigesimal: promedio bajo la mínima. Literal: la mitad o más bajo la mínima. */
    static boolean areaDesaprobada(EscalaCalificacion escala, List<String> notas, String minima) {
        if (notas.isEmpty()) {
            return false;
        }
        if (escala == EscalaCalificacion.VIGESIMAL) {
            Integer promedio = Calificaciones.promedio(notas);
            return promedio != null && Calificaciones.desaprobada(escala, String.valueOf(promedio), minima);
        }
        long bajo = notas.stream().filter(n -> Calificaciones.desaprobada(escala, n, minima)).count();
        return bajo * 2 >= notas.size();
    }
}
