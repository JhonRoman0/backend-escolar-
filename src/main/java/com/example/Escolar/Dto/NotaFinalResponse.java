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

    /** promedio: solo en escala vigesimal, redondeado desde ,5. */
    public record Celda(Integer idCompetencia, String calificacion, String conclusionDescriptiva, String registradoPor,
                        List<Guia> guia, Integer promedio) {
    }

    public record Alumno(Integer idMatricula, String codigo, String nombre, List<Celda> notas) {
    }

    /**
     * periodoEditable: año vigente con el periodo abierto o cerrado. puedeRegistrar y
     * puedeCorregir: además, el permiso de cada acción. requiereCodigo: periodo cerrado.
     */
    public record Detalle(Curso curso, EscalaCalificacion escala, Periodo periodo, boolean periodoEditable,
                          boolean puedeRegistrar,
                          boolean requiereCodigo, boolean puedeCorregir, List<CompetenciaFinal> competencias,
                          List<Alumno> alumnos) {
    }
}
