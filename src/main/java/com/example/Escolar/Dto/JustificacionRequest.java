package com.example.Escolar.Dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class JustificacionRequest {
    @NotBlank(message = "El motivo es obligatorio")
    private String motivo;
    private String documentoUrl;
    @NotNull(message = "La fecha de justificación es obligatoria")
    private LocalDate fechaJustificacion;
    private Long accesoId;
}