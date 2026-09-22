package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class TurnoRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    @NotNull(message = "La hora de entrada es obligatoria")
    private LocalTime horaEntrada;
    @NotNull(message = "La hora limite de entrada es obligatoria")
    private LocalTime horaEntradaLimite;
    @NotNull(message = "La hora limite de falta es obligatoria")
    private LocalTime horaFaltaLimite;
    @NotNull(message = "La hora de salida es obligatoria")
    private LocalTime horaSalida;
    private Long accesoId;
}