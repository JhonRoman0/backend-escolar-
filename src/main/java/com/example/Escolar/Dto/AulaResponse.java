package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AulaResponse {
    private Integer idAula;
    private String nombre;
    private Integer capacidad;
    private Long accesoId;
}