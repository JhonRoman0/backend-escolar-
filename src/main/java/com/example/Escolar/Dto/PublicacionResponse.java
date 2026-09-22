package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PublicacionResponse {
    private Integer idPublicacion;
    private Integer idUsuario;
    private String autor;
    private String titulo;
    private String slug;
    private String contenido;
    private String imagenPortadaUrl;
    private String categoria;
    private Byte esDestacado;
    private Byte estado;
    private LocalDateTime fechaPublicacion;
    private LocalDateTime fechaActualizacion;
    private Long accesoId;
}