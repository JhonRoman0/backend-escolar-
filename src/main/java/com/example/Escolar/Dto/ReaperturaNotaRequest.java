package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Reabre una nota final (del periodo o, sin periodo, del año). */
public record ReaperturaNotaRequest(
        @NotNull Integer idGradoSeccion,
        @NotNull Integer idArea,
        Integer periodo,
        @NotNull Integer idMatricula,
        @NotNull Integer idCompetencia,
        @NotBlank(message = "Escribe el motivo")
        @Size(max = 500, message = "El motivo no puede pasar de 500 caracteres") String motivo) {
}
