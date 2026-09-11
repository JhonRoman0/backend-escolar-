package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class AsistenciaResponse {
    private Integer idAsistencia;
    private Integer idMatricula;
    private Integer idAlumno;
    private String codigo;
    private String alumno;
    private String grado;
    private String seccion;
    private String turno;
    private String anio;
    private String urlFoto;
    private String estado;
    private LocalTime horaEntrada;
    private LocalTime horaSalida;
    private Integer idJustificacion;
    private String justificacion;
    private Integer idUsuarioRegistro;
    private String usuarioRegistro;
}