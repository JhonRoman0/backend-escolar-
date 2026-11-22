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

    @org.junit.jupiter.api.Test
    void lasNotasAnualesTienenSuPropioPermiso() {
        var registro = new ApiPermisoRegistro();
        assertEquals("CREAR", registro.buscar("PUT", "/notas/anuales").orElseThrow().accionCodigo());
        assertEquals("ACTUALIZAR", registro.buscar("PUT", "/notas/anuales/correccion").orElseThrow().accionCodigo());
        assertEquals("ACTUALIZAR", registro.buscar("POST", "/notas/anuales/reapertura").orElseThrow().accionCodigo());
        assertEquals(java.util.Set.of("NOTAS_ANUALES"), registro.buscar("GET", "/notas/anuales").orElseThrow().permisosCodigo());
        assertEquals("ACTUALIZAR", registro.buscar("POST", "/notas/finales/reapertura").orElseThrow().accionCodigo());
    }

    @org.junit.jupiter.api.Test
    void elResumenDeUnaEvaluacionExigeSuPropiaAccion() {
        assertEquals("VER_RESUMEN",
                new ApiPermisoRegistro().buscar("GET", "/evaluaciones/50/resumen").orElseThrow().accionCodigo());
    }

    @org.junit.jupiter.api.Test
    void exportarEvaluacionTieneSuPropiaAccion() {
        assertEquals(java.util.Set.of("CREAR", "ACTUALIZAR", "ELIMINAR", "IMPRIMIR_EXPORTAR", "VER_RESUMEN"),
                new ApiPermisoRegistro().accionesDe("EVALUACIONES"));
    }

    @org.junit.jupiter.api.Test
    void elHorarioDelColegioPideVerColegioYElPropioSoloLectura() {
        var registro = new ApiPermisoRegistro();
        assertEquals("VER_COLEGIO", registro.buscar("GET", "/horarios").orElseThrow().accionCodigo());
        assertEquals("VER_COLEGIO", registro.buscar("GET", "/horarios/seccion/5/recreos").orElseThrow().accionCodigo());
        assertEquals(null, registro.buscar("GET", "/horarios/mio").orElseThrow().accionCodigo());
        assertEquals(java.util.Set.of("IMPRIMIR_EXPORTAR", "VER_COLEGIO"), registro.accionesDe("HORARIOS"));
    }
}
