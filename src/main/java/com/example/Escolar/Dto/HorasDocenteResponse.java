package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HorasDocenteResponse {
    private Integer idDocente;
    private String docente;
    private double horasSemana;
    private double horasMes;
}