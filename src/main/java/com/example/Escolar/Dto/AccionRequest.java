package com.example.Escolar.Dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccionRequest {
    @NotBlank(message = "El código de la acción es obligatorio")
    private String codigo;
    @NotBlank(message = "El nombre de la acción es obligatorio")
    private String nombre;
    @Min(value = 0, message = "El acceso debe ser 0 (inactivo), 1 (activo) o 2 (eliminado)")
    @Max(value = 2, message = "El acceso debe ser 0 (inactivo), 1 (activo) o 2 (eliminado)")
    private Byte acceso;
}
