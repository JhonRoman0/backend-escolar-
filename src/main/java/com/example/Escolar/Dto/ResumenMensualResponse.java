package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResumenMensualResponse {
    private Integer idAlumno;
    private String alumno;
    private String grado;
    private String seccion;
    private Integer idMatricula;
    private Long diasAsistidos;
    private Long inasistencias;
    private Double porcentajeAsistencia;
}