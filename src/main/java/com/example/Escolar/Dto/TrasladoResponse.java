package com.example.Escolar.Dto;

import java.time.LocalDate;
import java.util.List;

/** Alumno trasladado con las notas de otra sección que puede usar en una evaluación. */
public record TrasladoResponse(Integer idMatricula, String codigo, String nombre, List<Opcion> opciones) {

    public record Opcion(Integer idNotaOrigen, String seccion, String evaluacion, LocalDate fecha,
                         String calificacion, List<EvaluacionResponse.Competencia> competencias) {
    }
}
