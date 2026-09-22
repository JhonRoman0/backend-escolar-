package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AnioEscolarRequest {
    @NotBlank(message = "El año es obligatorio")
    private String anio;
    @Min(value = 1, message = "El estado debe ser 1 (activo) o 2 (cerrado)")
    @Max(value = 2, message = "El estado debe ser 1 (activo) o 2 (cerrado)")
    private Byte estado;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Boolean bloqueoHorariosPorFecha;
    private Long accesoId;
}