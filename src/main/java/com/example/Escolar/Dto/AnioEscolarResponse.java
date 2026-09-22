package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AnioEscolarResponse {
    private Integer idAnio;
    private String anio;
    private byte estado;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Boolean bloqueoHorariosPorFecha;
    private Long accesoId;
}