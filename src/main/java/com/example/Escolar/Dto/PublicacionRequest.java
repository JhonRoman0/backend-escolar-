package com.example.Escolar.Dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicacionRequest {
    @NotBlank(message = "El título es obligatorio")
    private String titulo;
    private String slug;
    private String contenido;
    private String imagenPortadaUrl;
    private String categoria;
    @Min(value = 0, message = "esDestacado debe ser 0 o 1")
    @Max(value = 1, message = "esDestacado debe ser 0 o 1")
    private Byte esDestacado;
    @Min(value = 0, message = "estado debe ser 0 (borrador) o 1 (publicado)")
    @Max(value = 1, message = "estado debe ser 0 (borrador) o 1 (publicado)")
    private Byte estado;
    private Long accesoId;
}