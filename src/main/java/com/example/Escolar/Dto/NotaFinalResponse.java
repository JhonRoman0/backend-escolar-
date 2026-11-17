package com.example.Escolar.Dto;

import com.example.Escolar.Model.EscalaCalificacion;
import com.example.Escolar.Model.TipoPeriodo;

import java.time.LocalDate;
import java.util.List;

public final class NotaFinalResponse {
    private NotaFinalResponse() {
    }

    /** Un área (o transversal) de una sección con las competencias que el usuario califica. */
    public record Curso(Integer idGradoSeccion, Integer idArea, String area, boolean transversal, String nivel,
                        String grado, String seccion, String turno, boolean anioVigente, EscalaCalificacion escala,
                        TipoPeriodo tipoPeriodo, List<PeriodoEvaluacionResponse> periodos,
                        List<EvaluacionResponse.Competencia> competencias) {
    }

    public record Periodo(int numero, String nombre, String estado) {
    }

    /** sinEvaluaciones: subcursos ligados a la competencia que aún no la evaluaron en el periodo. */
    public record CompetenciaFinal(Integer idCompetencia, String codigo, String nombre, List<String> sinEvaluaciones) {
    }

    /** Nota de una evaluación que sirve de guía. */
    public record Guia(String evaluacion, String curso, LocalDate fecha, String calificacion) {
    }

    /** Nota de un alumno trasladado puesta en otra sección y no usada aquí: solo de referencia. */
    public record Referencia(String evaluacion, String curso, String seccion, LocalDate fecha, String calificacion) {
    }

    /**
     * promedio: solo en escala vigesimal, redondeado desde ,5. seccionOrigen: la nota
     * final se registró cuando el alumno estaba en otra sección. soloDireccion: periodo
     * cerrado que el alumno cursó en otra sección sin nota final; la registra quien corrige.
     * conNotaAnual: ya tiene la nota del año en la competencia; no se corrige ni reabre.
     */
    public record Celda(Integer idCompetencia, String calificacion, String conclusionDescriptiva, String registradoPor,
                        List<Guia> guia, Integer promedio, String seccionOrigen, List<Referencia> referencias,
                        boolean soloDireccion, boolean conNotaAnual) {
    }

    public record Alumno(Integer idMatricula, String codigo, String nombre, List<Celda> notas) {
    }

    /**
     * periodoEditable: año vigente con el periodo abierto o cerrado. puedeRegistrar y
     * puedeCorregir: además, el permiso de cada acción. Con el periodo cerrado solo
     * escribe quien corrige.
     */
    public record Detalle(Curso curso, EscalaCalificacion escala, Periodo periodo, boolean periodoEditable,
                          boolean puedeRegistrar,
                          boolean puedeCorregir, List<CompetenciaFinal> competencias,
                          List<Alumno> alumnos) {
    }
}
