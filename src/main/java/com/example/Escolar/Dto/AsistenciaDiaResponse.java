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
    // Filtros académicos como en alumnos (vigente)
    private Integer idNivel;
    private String nivel;
    private Integer idGrado;
    private Integer idSeccion;
    private Integer idTurno;
    private String turno;
    private Integer idGradoSeccion;
    private Integer idAnio;
    private String anio;
}