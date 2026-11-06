package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class AlumnoReporteResponse {
    private Integer idAlumno;
    private String nombre;
    private String codigo;
    private String documentoIdentidad;
    private LocalDate fechaNacimiento;
    private LocalDate fechaIngreso;
    private String anio;
    private String nivel;
    private String gradoSeccion;
    private String turno;
    /** null si quien descarga no tiene el permiso de apoderados. */
    private List<String> apoderados;
}
