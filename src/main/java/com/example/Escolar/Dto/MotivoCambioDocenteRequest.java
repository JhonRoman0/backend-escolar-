package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MotivoCambioDocenteRequest(
        @NotBlank String nombre,
        @NotNull Boolean aplicaTemporal,
        @NotNull Boolean aplicaDefinitivo
) {
}
