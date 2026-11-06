package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CursoEstadoRequest {
    @NotNull(message = "El id de acceso es obligatorio")
    private Long accesoId;
}
