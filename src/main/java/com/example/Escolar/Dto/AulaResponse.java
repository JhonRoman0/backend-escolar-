package com.example.Escolar.Dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AulaResponse {
    private Integer idAula;
    private String nombre;
    private Integer capacidad;
    private String tipo;
    /** Secciones de las que es salon ("3.° A – Mañana 2027"). */
    private List<String> secciones;
    private Long accesoId;
}