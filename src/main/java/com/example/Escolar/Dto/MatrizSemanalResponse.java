package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MatrizSemanalResponse {
    private Integer idAlumno;
    private String alumno;
    private String grado;
    private String seccion;
    private List<String> estados;
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