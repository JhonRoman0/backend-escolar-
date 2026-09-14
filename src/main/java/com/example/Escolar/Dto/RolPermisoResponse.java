package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RolPermisoResponse {
    private Integer idRolPermiso;
    private RolResponse rol;
    private PermisoResponse permiso;
    private List<String> acciones;
    private byte acceso;
}
