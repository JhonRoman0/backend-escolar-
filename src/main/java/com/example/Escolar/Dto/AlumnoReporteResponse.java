package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AlumnoReporteResponse {
    private Integer idAlumno;
    private String nombre;
    private String codigo;
    private String documentoIdentidad;
    private LocalDate fechaNacimiento;
    private LocalDate fechaIngreso;
    private String gradoSeccion;
    private String apoderado;
}
