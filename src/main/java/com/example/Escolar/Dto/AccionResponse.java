package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccionResponse {
    private Integer idAccion;
    private String codigo;
    private String nombre;
    private Long accesoId;
}
