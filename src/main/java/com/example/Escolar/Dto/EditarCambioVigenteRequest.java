package com.example.Escolar.Dto;

import java.time.LocalDate;

public record EditarCambioVigenteRequest(
        Integer idMotivo,
        String motivoDetalle,
        LocalDate fechaFin
) {
}
