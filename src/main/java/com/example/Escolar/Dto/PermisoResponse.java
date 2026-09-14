package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PermisoResponse {
    private Integer idPermiso;
    private String codigo;
    private String nombre;
    private ModuloResponse modulo;
    private byte acceso;
    private List<String> acciones;
}
