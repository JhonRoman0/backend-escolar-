package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class NotaReporteResponse {
    private LocalDate fecha;
    private String alumno;
    private String codigo;
    private String curso;
    private String competencia;
    private Integer bimestre;
    private String calificacion;
    private String docente;
}
