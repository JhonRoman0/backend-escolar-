package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class EventoResponse {
    private Integer idEvento;
    private String titulo;
    private String descripcion;
    private String lugar;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Byte esPublico;
    private String imagenUrl;
    private Long accesoId;
}