package com.example.Escolar.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** motivo: obligatorio al corregir una nota ya registrada (queda en el historial). */
public record NotaFinalRequest(
        @NotNull Integer idGradoSeccion,
        @NotNull Integer idArea,
        @NotNull Integer periodo,
        @NotNull List<@Valid Item> notas,
        @Size(max = 500, message = "El motivo no puede pasar de 500 caracteres") String motivo) {

    public NotaFinalRequest(Integer idGradoSeccion, Integer idArea, Integer periodo, List<Item> notas) {
        this(idGradoSeccion, idArea, periodo, notas, null);
    }

    /** En vigesimal la calificación la calcula el sistema: la que llegue se ignora. */
    public record Item(@NotNull Integer idMatricula, @NotNull Integer idCompetencia, String calificacion,
                       String conclusionDescriptiva) {
    }
}
