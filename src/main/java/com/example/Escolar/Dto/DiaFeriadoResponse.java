package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DiaFeriadoResponse {
    private Integer idDiaFeriado;
    private LocalDate fecha;
    private String motivo;
    private Integer idAnioEscolar;
    private String anio;
    private Long accesoId;
}