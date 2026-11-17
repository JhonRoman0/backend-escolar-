package com.example.Escolar.Dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class JustificacionRequest {
    @NotBlank(message = "El motivo es obligatorio")
    private String motivo;
    private Long accesoId;
}