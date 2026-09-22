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
}