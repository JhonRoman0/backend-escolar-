package com.example.Escolar.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class GaleriaRequest {
    @NotBlank(message = "El título es obligatorio")
    private String titulo;
    private String descripcion;
    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;
    @Valid
    private List<GaleriaDetalleRequest> detalles;
    @Min(value = 0, message = "El acceso debe ser 0 (inactivo), 1 (activo) o 2 (eliminado)")
    @Max(value = 2, message = "El acceso debe ser 0 (inactivo), 1 (activo) o 2 (eliminado)")
    private Byte acceso;
}