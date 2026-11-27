package com.example.Escolar.Security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void laPantallaDeAccesosSoloDependeDeRolesYPermisos() {
        var registro = new ApiPermisoRegistro();
        for (String ruta : java.util.List.of("/modulos", "/permisos", "/acciones", "/roles-permiso", "/roles-permiso/rol/3")) {
            assertEquals(java.util.Set.of("ROLES_PERMISOS"), registro.buscar("GET", ruta).orElseThrow().permisosCodigo(), ruta);
        }
        assertEquals("ACTUALIZAR", registro.buscar("POST", "/roles-permiso").orElseThrow().accionCodigo());
        assertEquals("ACTUALIZAR", registro.buscar("PUT", "/roles-permiso/4").orElseThrow().accionCodigo());
        assertEquals("ACTUALIZAR", registro.buscar("DELETE", "/roles-permiso/4").orElseThrow().accionCodigo());
        // El catálogo no se edita por API.
        for (String metodo : java.util.List.of("POST", "PUT", "DELETE")) {
            assertTrue(registro.buscar(metodo, "/modulos/1").isEmpty(), metodo);
            assertTrue(registro.buscar(metodo, "/permisos/1").isEmpty(), metodo);
            assertTrue(registro.buscar(metodo, "/acciones/1").isEmpty(), metodo);
        }
        // El formulario de usuarios lee los roles con su propio permiso.
        assertTrue(registro.buscar("GET", "/roles").orElseThrow().permisosCodigo().contains("USUARIOS"));
    }
}
