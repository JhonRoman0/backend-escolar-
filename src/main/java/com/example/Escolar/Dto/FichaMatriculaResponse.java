package com.example.Escolar.Dto;

import java.time.LocalDate;
import java.util.List;

/** Datos de la ficha de un alumno, vista desde una de sus matrículas. */
public record FichaMatriculaResponse(
        AlumnoFicha alumno,
        TrayectoriaFicha matricula,
        LocalDate fechaRegistro,
        String registradoPor,
        List<ApoderadoFicha> apoderados,
        List<TrayectoriaFicha> trayectoria) {

    public record AlumnoFicha(String codigo, String documentoIdentidad, String nombre, String sexo,
                              LocalDate fechaNacimiento, String direccion, String discapacidad, String urlFoto) {
    }

    public record ApoderadoFicha(String nombre, String documentoIdentidad, String parentesco, String celular,
                                 String gmail, String direccion, boolean principal) {
    }

    public record TrayectoriaFicha(Integer idMatricula, String anio, String nivel, String gradoSeccion,
                                   String turno, String estado) {
    }
}
