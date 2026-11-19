package com.example.Escolar.Security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Qué peticiones avisan a los navegadores conectados. */
class AvisoCambiosFilterTest {

    @Test
    void guardarBienAvisaElModuloDeLaRuta() {
        assertEquals("turnos", AvisoCambiosFilter.moduloQueCambio("PUT", "/turnos/1", 200));
        assertEquals("asistencias", AvisoCambiosFilter.moduloQueCambio("POST", "/asistencias/confirmar", 201));
        assertEquals("roles-permiso", AvisoCambiosFilter.moduloQueCambio("DELETE", "/roles-permiso/5", 204));
        assertEquals("notas", AvisoCambiosFilter.moduloQueCambio("PATCH", "/notas/finales", 200));
    }

    @Test
    void consultarNoAvisa() {
        assertNull(AvisoCambiosFilter.moduloQueCambio("GET", "/turnos", 200));
    }

    @Test
    void unErrorNoAvisa() {
        assertNull(AvisoCambiosFilter.moduloQueCambio("POST", "/turnos", 400));
        assertNull(AvisoCambiosFilter.moduloQueCambio("PUT", "/turnos/1", 500));
    }

    @Test
    void lasRutasQueNoGuardanNoAvisan() {
        assertNull(AvisoCambiosFilter.moduloQueCambio("POST", "/auth/login", 200));
        assertNull(AvisoCambiosFilter.moduloQueCambio("POST", "/auth/forgot-password", 200));
        assertNull(AvisoCambiosFilter.moduloQueCambio("POST", "/asistencias/previsualizar", 200));
    }
}
