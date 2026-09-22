package com.example.Escolar.Dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PermisoRequest {
    @NotBlank(message = "El código del permiso es obligatorio")
    private String codigo;
    @NotBlank(message = "El nombre del permiso es obligatorio")
    private String nombre;
    @NotNull(message = "El módulo es obligatorio")
    private Integer idModulo;
    private List<String> acciones;
    private Long accesoId;
}
