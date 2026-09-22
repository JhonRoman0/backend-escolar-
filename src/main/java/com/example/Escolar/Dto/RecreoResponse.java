package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class RecreoResponse {
    private Integer idRecreo;
    private Integer idNivel;
    private String nivel;
    private Integer idGradoSeccion;
    private String gradoSeccion;
    private Byte diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Long accesoId;
}
