package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CursoSubcursosRequest {
    @NotNull(message = "Indica si el área tiene subcursos")
    private Boolean tieneSubcursos;
}
