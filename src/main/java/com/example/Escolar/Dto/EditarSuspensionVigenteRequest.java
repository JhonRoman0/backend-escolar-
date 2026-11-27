package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record EditarSuspensionVigenteRequest(
        @NotBlank String motivo,
        String motivoDetalle,
        LocalDate fechaFin
) {
}
