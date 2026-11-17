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
    /** Hoy, aún dentro del límite de tardanza de su turno y sin marcar. */
    private Long pendientes;
    private Long totalEsperado;
    private Double porcentajeAsistencia;
}