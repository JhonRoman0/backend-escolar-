package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class MatriculaResponse {
    private Integer idMatricula;
    private Integer idAlumno;
    private Integer idAlumnoApoderado;
    private String codigoAlumno;
    private String alumno;
    private String apoderado;
    private Integer idUsuario;
    private String usuarioRegistro;
    private Integer idGradoSeccion;
    private String grado;
    private String seccion;
    private String turno;
    private Integer idAnio;
    private String anio;
    private Byte solicitudMatricula;
    private LocalDate fechaPago;
    private BigDecimal montoPago;
    private Byte acceso;
    private List<HistorialResponse> historial;
}