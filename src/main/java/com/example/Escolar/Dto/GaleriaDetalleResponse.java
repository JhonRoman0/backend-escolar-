package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GaleriaDetalleResponse {
    private Integer idDetalle;
    private String imagenUrl;
    private Integer orden;
}