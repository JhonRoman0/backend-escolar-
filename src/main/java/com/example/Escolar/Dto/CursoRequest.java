package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CursoRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    private Byte acceso;
}