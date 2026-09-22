package com.example.Escolar.Dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RolPermisoRequest {
    private Integer idRol;
    private Integer idPermiso;
    private List<String> acciones;
    private Long accesoId;
}
