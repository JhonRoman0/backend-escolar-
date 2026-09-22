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
public class DiaFeriadoRequest {
    @NotNull(message = "La fecha del feriado es obligatoria")
    private LocalDate fecha;
    @NotBlank(message = "El motivo es obligatorio")
    private String motivo;
    private Integer idAnioEscolar;
    private Long accesoId;
}