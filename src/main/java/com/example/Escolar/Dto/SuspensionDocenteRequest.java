package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SuspensionDocenteRequest {
    @NotNull(message = "El docente es obligatorio")
    private Integer idDocente;
    private Integer idSustituto;
    @NotBlank(message = "El motivo es obligatorio")
    private String motivo;
    private String motivoDetalle;
    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
}
