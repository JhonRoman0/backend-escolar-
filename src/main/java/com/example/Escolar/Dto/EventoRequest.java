package com.example.Escolar.Dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class EventoRequest {
    @NotBlank(message = "El título es obligatorio")
    private String titulo;
    private String descripcion;
    private String lugar;
    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    @Min(value = 0, message = "esPublico debe ser 0 o 1")
    @Max(value = 1, message = "esPublico debe ser 0 o 1")
    private Byte esPublico;
    @Min(value = 0, message = "El acceso debe ser 0 (inactivo), 1 (activo) o 2 (eliminado)")
    @Max(value = 2, message = "El acceso debe ser 0 (inactivo), 1 (activo) o 2 (eliminado)")
    private Byte acceso;
}