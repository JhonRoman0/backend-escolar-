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
    @Min(value = 0, message = "El acceso debe ser 0 (inactivo), 1 (activo) o 2 (eliminado)")
    @Max(value = 2, message = "El acceso debe ser 0 (inactivo), 1 (activo) o 2 (eliminado)")
    private Byte acceso;
}
