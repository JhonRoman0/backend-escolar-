package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AutorizacionRegistroResponse {
    private Integer idNotaAutorizacion;
    private String codigo;
    private String emisor;
    private String destinatario;
    private LocalDateTime fechaAsignacion;
    private LocalDateTime fechaGeneracion;
    private LocalDateTime fechaExpiracion;
    private String estado;
    private String consumidor;
    private LocalDateTime fechaUso;
}