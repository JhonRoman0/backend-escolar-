package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class HistorialResponse {
    private Integer idHistorial;
    private Integer idGradoSeccion;
    private String grado;
    private String seccion;
    private String turno;
    private Integer idAnio;
    private LocalDate fechaInicio;
    private LocalDate fechaFinal;
    private String motivo;
}