package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RolRequest {
    @NotBlank(message = "El nombre del rol es obligatorio")
    private String nombre;
    private String color;
    private Long accesoId;
}
