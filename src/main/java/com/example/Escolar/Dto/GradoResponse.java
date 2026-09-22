package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GradoResponse {
    private Integer idGrado;
    private String nombre;
    private Integer idNivel;
    private String nivel;
    private Long accesoId;
    private Integer idAnio;
    private String anio;
    private Integer idTurno;
    private String turno;
    private Integer idGradoSeccionDefault;
    private List<SeccionResponse> secciones;
}