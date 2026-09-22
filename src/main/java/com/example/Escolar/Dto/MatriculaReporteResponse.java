package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class MatriculaReporteResponse {
    private Integer idMatricula;
    private String alumno;
    private String codigo;
    private String gradoSeccion;
    private LocalDate fechaRegistro;
    private LocalDate fechaPago;
    private BigDecimal montoPago;
    private String registradoPor;
}
