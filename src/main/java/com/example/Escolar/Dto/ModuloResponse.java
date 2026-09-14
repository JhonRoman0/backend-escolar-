package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ModuloResponse {
    private Integer idModulo;
    private String modulo;
    private String icono;
    private byte acceso;
    private List<PermisoResponse> permisos;
}
