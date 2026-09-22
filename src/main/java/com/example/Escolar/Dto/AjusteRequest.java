package com.example.Escolar.Dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AjusteRequest {
    @NotBlank(message = "La clave es obligatoria")
    private String clave;
    @NotBlank(message = "El valor es obligatorio")
    private String valor;
    private Long accesoId;
}