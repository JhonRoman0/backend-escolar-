package com.example.Escolar.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Calificación vacía o null: se borra la nota de ese alumno. */
public record NotasEvaluacionRequest(@NotNull List<@Valid Item> notas) {
    public record Item(@NotNull Integer idMatricula, String calificacion) {
    }
}
