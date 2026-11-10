package com.example.Escolar.Dto;
import jakarta.validation.constraints.*;
public record ReconocimientoCalificacionRequest(@NotNull Integer idNotaOrigen,
        @NotNull Integer idEvaluacionDestino, @NotBlank String calificacionOrigen) {}
