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
}