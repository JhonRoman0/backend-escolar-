package com.example.Escolar.Dto;

import java.time.LocalDateTime;

/** Un navegador desde el que el usuario inició sesión. */
public record DispositivoResponse(
        Integer idDispositivo,
        String navegador,
        String ip,
        LocalDateTime primerUso,
        LocalDateTime ultimoUso,
        /** Vacío si no es de confianza (o la confianza venció). */
        LocalDateTime confiableHasta,
        /** Es el navegador desde el que se consulta. */
        boolean actual) {
}
