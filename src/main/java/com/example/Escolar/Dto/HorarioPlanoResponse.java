package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class HorarioPlanoResponse {
    private Integer idHorario;
    private Byte diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Integer idAula;
    private String aula;
    private String curso;
    private Integer idDocente;
    private String docente;
    private Integer idGradoSeccion;
    private String grado;
    private String seccion;
    private String turno;
    private Integer idAnio;
}
