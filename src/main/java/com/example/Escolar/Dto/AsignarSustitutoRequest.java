package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotNull;

public record AsignarSustitutoRequest(@NotNull Integer idDocenteSustituto) {
}
