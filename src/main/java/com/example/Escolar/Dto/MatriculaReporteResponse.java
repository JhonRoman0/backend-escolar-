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
    private String documentoIdentidad;
    private String anio;
    private String nivel;
    private String gradoSeccion;
    private String turno;
    private LocalDate fechaRegistro;
    private LocalDate fechaPago;
    private String metodoPago;
    /** Lo que corresponde pagar por el nivel en ese año. */
    private BigDecimal monto;
    private BigDecimal montoPago;
    private BigDecimal saldo;
    private String registradoPor;
    private String estado;
}
