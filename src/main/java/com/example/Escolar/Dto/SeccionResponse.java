package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeccionResponse {
    private Integer idSeccion;
    private String nombre;
    private Integer idGradoSeccion;

    /**
     * El turno y el anio viajan por seccion y no por grado: un mismo grado puede
     * tener 1ro A en la manana y 1ro B en la tarde, y cada fila necesita los suyos.
     */
    private Integer idTurno;
    private String turno;
    private Integer idAnio;
    private String anio;

    /** Conteo de uso en vivo: la UI deshabilita la papelera cuando hay registros. */
    private boolean tieneMatriculas;
    private boolean tieneAsignaciones;
}