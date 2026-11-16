package com.example.Escolar.Dto;

import com.example.Escolar.Dto.NotaFinalRequest.Item;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** motivo: obligatorio al corregir una nota ya registrada. En vigesimal la calificación se calcula. */
public record NotaAnualRequest(
        @NotNull Integer idGradoSeccion,
        @NotNull Integer idArea,
        @NotNull List<@Valid Item> notas,
        @Size(max = 500, message = "El motivo no puede pasar de 500 caracteres") String motivo) {
}
