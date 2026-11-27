package com.example.Escolar.Dto;

import com.example.Escolar.Model.TipoCambioDocente;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CambioDocenteRequest(
        @NotNull TipoCambioDocente tipo,
        @NotNull LocalDate fechaInicio,
        LocalDate fechaFin,
        @NotNull Integer idMotivo,
        String motivoDetalle,
        @NotEmpty List<@NotNull Integer> idsAsignacion,
        @NotNull(message = "El docente sustituto es obligatorio") Integer idDocenteSustituto
) {
}
