package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PermisoNestedRequest {
    @NotBlank(message = "El código del permiso es obligatorio")
    private String codigo;
    @NotBlank(message = "El nombre del permiso es obligatorio")
    private String nombre;
    private List<String> acciones;
}
