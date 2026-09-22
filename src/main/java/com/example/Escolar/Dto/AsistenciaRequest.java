package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AsistenciaRequest {
    private String codigo;
    private String codigoHash;
    private Integer idJustificacion;
}