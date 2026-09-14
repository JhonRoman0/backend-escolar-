package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnioEscolarRequest {
    @NotBlank(message = "El año es obligatorio")
    private String anio;
    @Min(value = 1, message = "El estado debe ser 1 (activo) o 2 (cerrado)")
    @Max(value = 2, message = "El estado debe ser 1 (activo) o 2 (cerrado)")
    private Byte estado;
    @Min(value = 0, message = "El acceso debe estar entre 0 y 2")
    @Max(value = 2, message = "El acceso debe estar entre 0 y 2")
    private Byte acceso;
}