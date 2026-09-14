package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GaleriaDetalleRequest {
    @NotBlank(message = "La URL de la imagen es obligatoria")
    private String imagenUrl;
    private Integer orden;
}