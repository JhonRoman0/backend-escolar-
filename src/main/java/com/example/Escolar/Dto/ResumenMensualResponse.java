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