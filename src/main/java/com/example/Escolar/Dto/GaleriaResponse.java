package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class GaleriaResponse {
    private Integer idGaleria;
    private String titulo;
    private String descripcion;
    private LocalDate fecha;
    private Long accesoId;
    private List<GaleriaDetalleResponse> detalles;
}