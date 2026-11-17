package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Anular una marca del día: el motivo queda en el historial. */
public record AnulacionAsistenciaRequest(
        @NotBlank(message = "Escribe el motivo de la anulación")
        @Size(min = 5, max = 200, message = "El motivo debe tener entre 5 y 200 caracteres") String motivo) {
}
