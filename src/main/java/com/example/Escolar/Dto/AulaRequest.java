package com.example.Escolar.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AulaRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    @NotNull(message = "La capacidad es obligatoria")
    @Min(value = 1, message = "La capacidad minima es 1")
    private Integer capacidad;
    private Long accesoId;
}