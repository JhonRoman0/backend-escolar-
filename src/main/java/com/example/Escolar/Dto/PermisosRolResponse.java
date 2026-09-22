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
}
