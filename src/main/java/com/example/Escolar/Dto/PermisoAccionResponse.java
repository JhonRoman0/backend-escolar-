package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PermisoAccionResponse {
    private Integer idPermiso;
    private String codigo;
    private String nombre;
    private List<String> accionesDisponibles;
    private List<String> accionesConcedidas;
}
