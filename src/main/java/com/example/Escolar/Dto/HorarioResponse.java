package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class HorarioResponse {
    private Integer idHorario;
    private Integer idAula;
    private String aula;
    private Byte diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
}