package com.example.Escolar.Dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RolPermisoRequest {
    private Integer idRol;
    private Integer idPermiso;
    private List<String> acciones;
    @Min(value = 0, message = "El acceso debe ser 0 (inactivo), 1 (activo) o 2 (eliminado)")
    @Max(value = 2, message = "El acceso debe ser 0 (inactivo), 1 (activo) o 2 (eliminado)")
    private Byte acceso;
}
