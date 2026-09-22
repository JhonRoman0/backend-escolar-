package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class TurnoResponse {
    private Integer idTurno;
    private String nombre;
    private LocalTime horaEntrada;
    private LocalTime horaEntradaLimite;
    private LocalTime horaFaltaLimite;
    private LocalTime horaSalida;
    private Long accesoId;
}