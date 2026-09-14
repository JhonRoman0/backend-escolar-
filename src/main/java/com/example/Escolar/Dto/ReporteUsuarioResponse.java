package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class ReporteUsuarioResponse {
    private LocalDate fecha;
    private LocalTime horaEntrada;
    private String alumno;
    private String gradoSeccion;
    private String estado;
    private String justificacion;
}