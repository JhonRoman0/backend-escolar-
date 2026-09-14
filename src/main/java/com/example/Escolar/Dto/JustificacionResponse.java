package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class JustificacionResponse {
    private Integer idJustificacion;
    private String motivo;
    private String documentoUrl;
    private LocalDate fechaJustificacion;
    private Byte acceso;
}