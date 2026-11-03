package com.example.Escolar.Service;

import java.time.LocalDate;
import java.time.ZoneId;

/** Reglas de nacimiento compartidas por los flujos que escriben datos personales. */
final class FechaNacimiento {
    static final LocalDate MINIMO_ALUMNO = LocalDate.of(2000, 1, 1);
    static final int EDAD_MINIMA_ADULTO = 18;
    private static final ZoneId ZONA_COLEGIO = ZoneId.of("America/Lima");

    private FechaNacimiento() {}

    static LocalDate hoy() {
        return LocalDate.now(ZONA_COLEGIO);
    }

    static void validarAlumno(LocalDate fecha, LocalDate hoy) {
        validarFecha(fecha, hoy);
        if (fecha.isBefore(MINIMO_ALUMNO)) {
            throw new IllegalArgumentException("La fecha de nacimiento debe ser a partir del 01/01/2000");
        }
    }

    static void validarAdulto(LocalDate fecha, LocalDate original, LocalDate hoy) {
        if (fecha != null && fecha.equals(original)) return;
        validarFecha(fecha, hoy);
        // Comparar con el límite mantiene el cumpleaños del 29/02 en el 01/03
        // de los años no bisiestos, igual que la comparación de mes y día del frontend.
        if (fecha.isAfter(hoy.minusYears(EDAD_MINIMA_ADULTO))) {
            throw new IllegalArgumentException("Debe tener al menos 18 años cumplidos");
        }
    }

    private static void validarFecha(LocalDate fecha, LocalDate hoy) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de nacimiento es obligatoria");
        }
        if (fecha.getYear() < 1 || fecha.getYear() > 9999) {
            throw new IllegalArgumentException("Escribe una fecha de nacimiento válida");
        }
        if (fecha.isAfter(hoy)) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura");
        }
    }
}
