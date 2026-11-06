package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/** Resultado de buscar al alumno por DNI en el paso 1. */
@Getter
@Setter
public class InscripcionAlumnoResponse {
    private boolean existe;
    private Integer idAlumno;
    private String documentoIdentidad;
    private String nombre;
    private String apellidoPat;
    private String apellidoMat;
    private LocalDate fechaNacimiento;
    private String sexo;
    private String direccion;
    private boolean tieneDiscapacidad;
    private String discapacidad;
    private String urlFoto;
    /** Ej. "1ro Primaria 2026"; null si nunca se matriculó. */
    private String ultimaMatricula;
    private List<ApoderadoBusquedaResponse> apoderados;
}
