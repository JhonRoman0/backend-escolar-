package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JustificarRequest {
    @NotNull(message = "El motivo de justificación es obligatorio")
    private Integer idJustificacion;
}