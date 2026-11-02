package com.example.Escolar.Security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Gana la primera ruta que coincide: una lectura general no debe tapar la del reporte. */
class ApiPermisoRegistroReportesTest {

    @ParameterizedTest
    @ValueSource(strings = {"/usuarios/reporte", "/docentes/reporte", "/alumnos/reporte", "/matriculas/reporte", "/notas/reporte"})
    void exportarExigeSuPropiaAccion(String ruta) {
        assertEquals("IMPRIMIR_EXPORTAR", new ApiPermisoRegistro().buscar("GET", ruta).orElseThrow().accionCodigo());
    }

    @org.junit.jupiter.api.Test
    void laFichaExigeSuPropiaAccion() {
        assertEquals("IMPRIMIR_FICHA",
                new ApiPermisoRegistro().buscar("GET", "/matriculas/5/ficha").orElseThrow().accionCodigo());
    }
}
