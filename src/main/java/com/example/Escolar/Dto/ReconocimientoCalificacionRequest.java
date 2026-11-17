package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** calificacionOrigen: la que vio el usuario; si cambió, se rechaza. */
public record ReconocimientoCalificacionRequest(@NotNull Integer idNotaOrigen,
        @NotNull Integer idEvaluacionDestino, @NotBlank String calificacionOrigen) {
}
