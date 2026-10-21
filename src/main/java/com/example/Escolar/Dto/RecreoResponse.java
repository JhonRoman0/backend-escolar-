package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class RecreoResponse {
    private Integer idRecreo;
    private Integer idAnio;
    private String anio;
    private Integer idNivel;
    private String nivel;
    private Integer idTurno;
    private String turno;
    private Integer idGrado;
    private String grado;
    private Byte diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Long accesoId;
}
