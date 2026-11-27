package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;

public record ResolverIncidenciaRequest(@NotBlank String resolucion) {
}
