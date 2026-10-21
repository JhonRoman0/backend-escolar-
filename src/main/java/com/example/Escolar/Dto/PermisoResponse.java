package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class PermisoResponse {
    private Integer idPermiso;
    private String codigo;
    private String nombre;
    private ModuloResponse modulo;
    private Long accesoId;
    private List<String> acciones;
    /** Nombre de cada accion en este modulo; si una falta se usa el generico. */
    private Map<String, String> etiquetas;
}
