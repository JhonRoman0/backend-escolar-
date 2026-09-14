package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AutorizacionResponse {
    private String codigo;
    private String destinatario;
    private LocalDateTime fechaGeneracion;
    private LocalDateTime fechaExpiracion;
}