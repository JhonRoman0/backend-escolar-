package com.example.Escolar.Service;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class FechaNacimientoTest {
    private final LocalDate hoy = LocalDate.of(2026, 11, 2);

    @Test
    void alumnoAdmiteLosLimitesYRechazaFechasFueraDelRango() {
        assertDoesNotThrow(() -> FechaNacimiento.validarAlumno(LocalDate.of(2000, 1, 1), hoy));
        assertDoesNotThrow(() -> FechaNacimiento.validarAlumno(hoy, hoy));
        assertEquals("La fecha de nacimiento debe ser a partir del 01/01/2000",
                assertThrows(IllegalArgumentException.class,
                        () -> FechaNacimiento.validarAlumno(LocalDate.of(1999, 12, 31), hoy)).getMessage());
        assertEquals("La fecha de nacimiento no puede ser futura",
                assertThrows(IllegalArgumentException.class,
                        () -> FechaNacimiento.validarAlumno(hoy.plusDays(1), hoy)).getMessage());
    }

    @Test
    void adultoDebeHaberCumplidoLos18ConsiderandoMesYDia() {
        assertDoesNotThrow(() -> FechaNacimiento.validarAdulto(hoy.minusYears(18), null, hoy));
        assertDoesNotThrow(() -> FechaNacimiento.validarAdulto(hoy.minusYears(18).minusDays(1), null, hoy));
        assertEquals("Debe tener al menos 18 años cumplidos",
                assertThrows(IllegalArgumentException.class,
                        () -> FechaNacimiento.validarAdulto(hoy.minusYears(18).plusDays(1), null, hoy)).getMessage());
    }

    @Test
    void conservarLaFechaOriginalNoPermiteCambiarlaPorOtraInvalida() {
        assertDoesNotThrow(() -> FechaNacimiento.validarAdulto(hoy, hoy, hoy));
        assertThrows(IllegalArgumentException.class,
                () -> FechaNacimiento.validarAdulto(hoy.minusDays(1), hoy, hoy));
        assertThrows(IllegalArgumentException.class, () -> FechaNacimiento.validarAdulto(hoy, null, hoy));
        assertThrows(IllegalArgumentException.class, () -> FechaNacimiento.validarAdulto(null, null, hoy));
    }

    @Test
    void elCumpleanosBisiestoEsElPrimeroDeMarzoEnAnioNoBisiesto() {
        LocalDate nacimiento = LocalDate.of(2008, 2, 29);
        assertThrows(IllegalArgumentException.class,
                () -> FechaNacimiento.validarAdulto(nacimiento, null, LocalDate.of(2026, 2, 28)));
        assertDoesNotThrow(() -> FechaNacimiento.validarAdulto(nacimiento, null, LocalDate.of(2026, 3, 1)));
        assertDoesNotThrow(() -> FechaNacimiento.validarAdulto(LocalDate.of(2006, 2, 28), null, LocalDate.of(2024, 2, 29)));
        assertThrows(IllegalArgumentException.class,
                () -> FechaNacimiento.validarAdulto(LocalDate.of(2006, 3, 1), null, LocalDate.of(2024, 2, 29)));
    }

    @Test
    void fechasFuturasVaciasYFueraDelFormatoAdmitidoSonRechazadas() {
        assertEquals("La fecha de nacimiento no puede ser futura", assertThrows(IllegalArgumentException.class,
                () -> FechaNacimiento.validarAdulto(hoy.plusDays(1), null, hoy)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> FechaNacimiento.validarAlumno(null, hoy));
        assertThrows(IllegalArgumentException.class, () -> FechaNacimiento.validarAdulto(LocalDate.of(0, 1, 1), null, hoy));
    }
}
