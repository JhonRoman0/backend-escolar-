package com.example.Escolar.Dto;

import com.example.Escolar.Model.EscalaCalificacion;
import com.example.Escolar.Model.TipoPeriodo;

import java.util.List;
import java.util.Map;

/**
 * periodos: hasta el periodo de la boleta, inclusive. conFinal: boleta del último
 * periodo con alguna nota final del año registrada (lleva la columna Final).
 */
public record BoletaResponse(Seccion seccion, EscalaCalificacion escala, TipoPeriodo tipoPeriodo,
                             List<PeriodoEvaluacionResponse> periodos, int periodo, List<Area> areas,
                             List<AlumnoBoleta> alumnos, boolean conFinal) {

    public record Seccion(String anio, String nivel, String grado, String seccion, String turno) {
    }

    public record Area(String nombre, boolean transversal, List<EvaluacionResponse.Competencia> competencias) {
    }

    public record Asistencia(int puntual, int tardanza, int justificada, int inasistencia) {
    }

    /**
     * notas: idCompetencia → periodo → calificación. conclusiones: idCompetencia → texto del periodo.
     * finales y conclusionesFinales: idCompetencia → nota del año y su conclusión (literal).
     */
    public record AlumnoBoleta(Integer idMatricula, String codigo, String documentoIdentidad, String nombre,
                               Map<Integer, Map<Integer, String>> notas, Map<Integer, String> conclusiones,
                               Asistencia asistencia, Map<Integer, String> finales,
                               Map<Integer, String> conclusionesFinales) {
    }
}
