package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class ReporteGeneralResponse {
    private LocalDate fecha;
    private LocalTime horaEntrada;
    private String codigo;
    private String alumno;
    private String gradoSeccion;
    private String estado;
    private String justificacion;
    private String registradoPor;
    private Integer idNivel;
    private String nivel;
    private Integer idGrado;
    private String grado;
    private Integer idSeccion;
    private String seccion;
    private Integer idTurno;
    private String turno;
    private Integer idGradoSeccion;
    private Integer idAnio;
    private String anio;
}