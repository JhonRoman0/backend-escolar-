package com.example.Escolar.Dto;

import java.time.LocalDate;

/** estado: POR_COMENZAR, ABIERTO o CERRADO según la fecha de hoy. */
public record PeriodoEvaluacionResponse(int numero, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
                                        String estado) {
}
