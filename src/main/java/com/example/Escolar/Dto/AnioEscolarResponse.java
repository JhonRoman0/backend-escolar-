package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnioEscolarResponse {
    private Integer idAnio;
    private String anio;
    private byte estado;
    private byte acceso;
}