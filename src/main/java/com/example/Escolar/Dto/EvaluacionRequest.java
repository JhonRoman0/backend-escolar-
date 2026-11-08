package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

/** idAsignacion y periodo solo se usan al crear. */
public record EvaluacionRequest(
        Integer idAsignacion,
        Integer periodo,
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede pasar de 100 caracteres") String nombre,
        @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
        @NotEmpty(message = "Marca al menos una competencia") Set<Integer> competencias) {
}
