package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class AsistenciaDiaResponse {
    private Integer idAlumno;
    private String alumno;
    private String grado;
    private String seccion;
    private Integer idAsistencia;
    private String estado;
    private LocalTime horaEntrada;
    private String marcadoPor;
}