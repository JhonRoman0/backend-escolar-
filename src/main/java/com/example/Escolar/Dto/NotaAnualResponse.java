package com.example.Escolar.Dto;

import com.example.Escolar.Model.EscalaCalificacion;

import java.util.List;

public final class NotaAnualResponse {
    private NotaAnualResponse() {
    }

    public record Periodo(int numero, String nombre) {
    }

    /** Nota final de un periodo; null si aún no la tiene. */
    public record NotaPeriodo(int numero, String calificacion) {
    }

    /**
     * faltan: periodos sin nota final (con alguno no se registra la del año).
     * promedio: en vigesimal, la nota del año que corresponde.
     */
    public record Celda(Integer idCompetencia, List<NotaPeriodo> periodos, List<String> faltan, String calificacion,
                        String conclusionDescriptiva, String registradoPor, Integer promedio) {
    }

    public record Alumno(Integer idMatricula, String codigo, String nombre, List<Celda> notas) {
    }

    /**
     * disponible: año vigente con el último periodo ya cerrado; si no, noDisponible
     * dice por qué. puedeRegistrar y puedeCorregir: además, el permiso de cada acción.
     */
    public record Detalle(NotaFinalResponse.Curso curso, EscalaCalificacion escala, List<Periodo> periodos,
                          boolean disponible, String noDisponible, boolean puedeRegistrar, boolean puedeCorregir,
                          List<EvaluacionResponse.Competencia> competencias, List<Alumno> alumnos) {
    }
}
