package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SuspensionDocenteResponse {
    private Integer idSuspension;
    private Integer idDocente;
    private String docente;
    private Integer idSustituto;
    private String sustituto;
    private String motivo;
    private String motivoDetalle;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String anio;
    private String estado;
    private LocalDate fechaFinalizacion;
    private String motivoResolucion;
    private Boolean incidencia;
    private String motivoIncidencia;
    private boolean puedeEditar;
    private boolean puedeCancelar;
    private boolean puedeFinalizar;
    private Long accesoId;
}
