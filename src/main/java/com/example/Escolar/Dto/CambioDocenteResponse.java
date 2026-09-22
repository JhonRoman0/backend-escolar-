package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CambioDocenteResponse {
    private Integer idCambio;
    private Integer idAsignacion;
    private Integer idDocenteAnterior;
    private String docenteAnterior;
    private Integer idDocenteNuevo;
    private String docenteNuevo;
    private String motivo;
    private String motivoDetalle;
    private LocalDate fechaCambio;
    private Integer idUsuarioRegistro;
    private String usuarioRegistro;
    private Long accesoId;
}
