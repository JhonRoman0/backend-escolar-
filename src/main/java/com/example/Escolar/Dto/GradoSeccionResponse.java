package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GradoSeccionResponse {
    private Integer idGradoSeccion;
    private Integer idSeccion;
    private String nombre;
    private Integer idTurno;
    private String turno;
}
