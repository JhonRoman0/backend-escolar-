package com.example.Escolar.Service;

import java.time.LocalDate;

final class RangoFechas {

    private RangoFechas() {
    }

    /** Sin límites entra todo; con límites, una fecha vacía queda fuera. */
    static boolean contiene(LocalDate fecha, LocalDate inicio, LocalDate fin) {
        if (inicio == null && fin == null) {
            return true;
        }
        return fecha != null
                && (inicio == null || !fecha.isBefore(inicio))
                && (fin == null || !fecha.isAfter(fin));
    }
}
