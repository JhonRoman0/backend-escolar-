package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EstadisticasResponse {
    private Long presentes;
    private Long tardanzas;
    private Long justificados;
    private Long inasistencias;
    private Long totalEsperado;
    private Double porcentajeAsistencia;
}