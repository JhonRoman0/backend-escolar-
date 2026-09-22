package com.example.Escolar.Dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class RecreoRequest {
    @NotNull(message = "El nivel es obligatorio")
    private Integer idNivel;
    private Integer idGradoSeccion;
    @NotNull(message = "El dia de la semana es obligatorio")
    @Min(value = 1, message = "El dia debe ser entre 1 (Lunes) y 7 (Domingo)")
    @Max(value = 7, message = "El dia debe ser entre 1 (Lunes) y 7 (Domingo)")
    private Byte diaSemana;
    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;
    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;
}
