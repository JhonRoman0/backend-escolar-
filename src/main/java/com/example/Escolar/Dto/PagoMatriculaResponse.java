package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class PagoMatriculaResponse {
    private Integer idPago;
    private String tipo;
    private BigDecimal monto;
    private String metodoPago;
    private String codigoTransaccion;
    private String urlComprobante;
    private LocalDateTime fecha;
    private String registradoPor;
}
