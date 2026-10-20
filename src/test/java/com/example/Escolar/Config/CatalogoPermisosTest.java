package com.example.Escolar.Config;

import com.example.Escolar.Security.ApiPermisoRegistro;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * El catalogo de accesos y las rutas protegidas deben decir lo mismo: si una
 * ruta pide una accion que el catalogo no ofrece, ningun rol (salvo ADMIN) la
 * puede usar; si el catalogo ofrece una accion que ninguna ruta pide, la
 * pantalla de accesos muestra una casilla que no hace nada.
 */
class CatalogoPermisosTest {

    private final ApiPermisoRegistro registro = new ApiPermisoRegistro();

    @Test
    void cadaPermisoDelCatalogoTieneLasMismasAccionesQueSusRutas() {
        CatalogoPermisos.todos().forEach((permiso, acciones) ->
                assertEquals(registro.accionesDe(permiso), new TreeSet<>(acciones.keySet()),
                        "Acciones de " + permiso + " (rutas vs catálogo)"));
    }

    @Test
    void cadaAccionTieneSuNombreEnElModulo() {
        for (Map.Entry<String, Map<String, String>> permiso : CatalogoPermisos.todos().entrySet()) {
            permiso.getValue().forEach((accion, etiqueta) ->
                    assertFalse(etiqueta.isBlank(), permiso.getKey() + " " + accion + " no tiene etiqueta"));
        }
    }

    @Test
    void cubreLosModulosRevisados() {
        assertEquals(new TreeSet<>(java.util.List.of("USUARIOS", "ROLES", "TURNOS", "AULAS", "ANIOS_ESCOLARES",
                        "GRADOS", "DOCENTES", "CURSOS", "COMPETENCIAS", "ASIGNACIONES", "RECREOS")),
                new TreeSet<>(CatalogoPermisos.todos().keySet()));
    }
}
