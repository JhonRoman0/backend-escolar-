package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MetodoPagoResponse {
    private Integer idMetodoPago;
    private String nombre;
    private boolean requiereCodigo;
    private boolean requiereComprobante;
    private boolean activo;
}
