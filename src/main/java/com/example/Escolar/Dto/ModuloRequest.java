package com.example.Escolar.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ModuloRequest {
    @NotBlank(message = "El nombre del módulo es obligatorio")
    private String modulo;
    private String icono;
    @Min(value = 0, message = "El acceso debe ser 0 (inactivo), 1 (activo) o 2 (eliminado)")
    @Max(value = 2, message = "El acceso debe ser 0 (inactivo), 1 (activo) o 2 (eliminado)")
    private Byte acceso;
    @Valid
    private List<PermisoNestedRequest> permisos;
}
