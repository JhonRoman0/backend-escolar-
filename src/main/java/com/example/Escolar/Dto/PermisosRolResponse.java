package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PermisosRolResponse {
    private Integer idRol;
    private String nombreRol;
    private List<ModuloPermisosResponse> modulos;
    // DOCENTE, APODERADO o null: con solo lectura el docente ve lo suyo y el apoderado lo de sus hijos.
    private String vinculo;
}
