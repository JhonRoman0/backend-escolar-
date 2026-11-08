package com.example.Escolar.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** codigoAutorizacion: solo con el periodo cerrado. Calificación vacía: se borra (solo al corregir). */
public record NotaFinalRequest(
        @NotNull Integer idGradoSeccion,
        @NotNull Integer idArea,
        @NotNull Integer periodo,
        String codigoAutorizacion,
        @NotNull List<@Valid Item> notas) {

    public record Item(@NotNull Integer idMatricula, @NotNull Integer idCompetencia, String calificacion,
                       String conclusionDescriptiva) {
    }
}
