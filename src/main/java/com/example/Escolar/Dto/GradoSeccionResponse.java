package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GradoSeccionResponse {
    private Integer idGradoSeccion;
    private Integer idSeccion;
    private String nombre;
    private Integer idTurno;
    private String turno;
    private Integer idAnio;
    private String anio;

    /** Conteo de uso en vivo: la UI deshabilita la papelera cuando hay registros. */
    private boolean tieneMatriculas;
    private boolean tieneAsignaciones;
}
