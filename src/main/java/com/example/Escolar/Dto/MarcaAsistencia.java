package com.example.Escolar.Dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Lo que los listados de asistencia usan de cada marca, leído como columnas sueltas:
 * cargar la entidad completa arrastraba ~20 tablas por fila (relaciones EAGER).
 */
public record MarcaAsistencia(Integer idMatricula, Integer idAsistencia, LocalDate fecha, LocalTime horaEntrada,
                              String estado, String justificacion, String acompanante, String urlEvidencia,
                              String registradorNombre, String registradorApellido) {

    public String registradoPor() {
        return registradorNombre + " " + registradorApellido;
    }
}
