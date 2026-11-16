package com.example.Escolar.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * idAsignacion y periodo solo se usan al crear. noRinden: alumnos trasladados
 * que no rinden la evaluación y usan una nota de su sección anterior.
 */
public record EvaluacionRequest(
        Integer idAsignacion,
        Integer periodo,
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede pasar de 100 caracteres") String nombre,
        @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
        @NotEmpty(message = "Marca al menos una competencia") Set<Integer> competencias,
        List<@Valid NoRinde> noRinden) {

    public EvaluacionRequest(Integer idAsignacion, Integer periodo, String nombre, LocalDate fecha, Set<Integer> competencias) {
        this(idAsignacion, periodo, nombre, fecha, competencias, null);
    }

    public record NoRinde(
            @NotNull(message = "Falta el alumno") Integer idMatricula,
            @NotNull(message = "Elige la nota que usará el alumno") Integer idNotaOrigen) {
    }
}
