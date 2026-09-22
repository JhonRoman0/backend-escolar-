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
    private Long accesoId;
}
