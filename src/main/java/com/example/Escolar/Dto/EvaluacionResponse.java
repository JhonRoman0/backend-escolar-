package com.example.Escolar.Dto;

import com.example.Escolar.Model.EscalaCalificacion;
import com.example.Escolar.Model.TipoPeriodo;

import java.time.LocalDate;
import java.util.List;

public final class EvaluacionResponse {
    private EvaluacionResponse() {
    }

    public record Competencia(Integer idCompetencia, String codigo, String nombre) {
    }

    /** Un curso de una sección que el usuario puede evaluar, con todo lo que la pantalla necesita. */
    public record Curso(Integer idAsignacion, String curso, String area, String nivel, String grado, String seccion,
                        String turno, String docente, Integer idAnio, boolean anioVigente, EscalaCalificacion escala,
                        TipoPeriodo tipoPeriodo, List<PeriodoEvaluacionResponse> periodos,
                        List<Competencia> competencias,
                        /** 1 = lunes … 7 = domingo. Vacío: el curso no tiene horario. */
                        List<Integer> diasClase,
                        List<LocalDate> feriados) {
    }

    /** alumnosConNotaFinal: alumnos cuya nota en esta evaluación ya no cambia (ver Alumno). */
    public record Resumen(Integer idEvaluacion, String nombre, LocalDate fecha, int periodo,
                          List<Competencia> competencias, int notasRegistradas, int totalAlumnos,
                          String registradoPor, boolean editable, int alumnosConNotaFinal, boolean tieneNotasRegistradas) {
    }

    /** notaFinalRegistrada: ya tiene nota final en alguna competencia que la evaluación afecta. */
    public record Alumno(Integer idMatricula, String codigo, String nombre, String calificacion,
                         boolean notaFinalRegistrada, boolean tieneHistorial,
                         ReconocimientoCalificacionResponse.Procedencia procedencia, boolean notaProtegida) {
    }

    public record Detalle(Resumen evaluacion, EscalaCalificacion escala, List<Alumno> alumnos, Integer idAsignacion) {
    }

    /** Cuántos alumnos de la sección ya tienen la nota final de la competencia en el periodo. */
    public record AvanceFinal(Integer idCompetencia, int conNotaFinal, int totalAlumnos) {
        public boolean completa() {
            return totalAlumnos > 0 && conNotaFinal >= totalAlumnos;
        }
    }

    /** notas por competencia: las de las evaluaciones que la afectan; promedio solo en vigesimal. */
    public record PorCompetencia(Integer idCompetencia, List<String> notas, Integer promedio) {
    }

    /** notas: idEvaluacion → calificación. */
    public record FilaRegistro(Integer idMatricula, String codigo, String nombre, java.util.Map<Integer, String> notas,
                               List<PorCompetencia> competencias, boolean tieneHistorial,
                               java.util.Map<Integer, ReconocimientoCalificacionResponse.Procedencia> procedencias) {
    }

    public record Registro(EscalaCalificacion escala, List<Resumen> evaluaciones, List<Competencia> competencias,
                           List<FilaRegistro> alumnos) {
    }
}
