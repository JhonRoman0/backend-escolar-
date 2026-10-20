package com.example.Escolar.Config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Acciones que tiene cada permiso y como se llaman en su modulo. La pantalla de
 * accesos muestra esta etiqueta en vez del nombre generico ("Crear"), porque en
 * cada modulo la accion significa otra cosa: en Cursos, crear es importar el
 * Excel del SIAGIE.
 *
 * Al arrancar, el DataSeeder sincroniza estos permisos con la base: agrega lo
 * que falta, actualiza las etiquetas y desactiva las acciones que ya no estan
 * aqui (tambien en los roles que las tenian). Por ahora solo cubre los modulos
 * revisados; los demas permisos siguen con su lista sembrada.
 *
 * CatalogoPermisosTest exige que cada accion coincida con las rutas que la
 * piden en ApiPermisoRegistro.
 */
public final class CatalogoPermisos {

    private static final Map<String, Map<String, String>> CATALOGO = new LinkedHashMap<>();

    static {
        permiso("USUARIOS",
                "CREAR", "Crear usuarios",
                "ACTUALIZAR", "Editar, cambiar foto y desbloquear",
                "ELIMINAR", "Eliminar usuarios",
                "IMPRIMIR_EXPORTAR", "Exportar reporte");
        permiso("ROLES",
                "CREAR", "Crear roles",
                "ACTUALIZAR", "Editar roles",
                "ELIMINAR", "Eliminar roles");
        permiso("TURNOS",
                "CREAR", "Crear turnos",
                "ACTUALIZAR", "Editar horarios del turno",
                "ELIMINAR", "Eliminar turnos");
        permiso("AULAS",
                "CREAR", "Crear aulas",
                "ACTUALIZAR", "Editar aulas",
                "ELIMINAR", "Eliminar aulas");
        permiso("ANIOS_ESCOLARES",
                "CREAR", "Crear años escolares",
                "ACTUALIZAR", "Editar fechas y cambiar el estado",
                "ELIMINAR", "Eliminar años escolares");
        permiso("GRADOS",
                "CREAR", "Crear grados y secciones",
                "ACTUALIZAR", "Editar grados y secciones",
                "ELIMINAR", "Eliminar grados y secciones");
        // Los docentes se crean desde Usuarios, al asignar el rol Docente.
        permiso("DOCENTES",
                "ACTUALIZAR", "Editar datos y foto",
                "ELIMINAR", "Eliminar docentes",
                "IMPRIMIR_EXPORTAR", "Exportar reporte");
        permiso("CURSOS",
                "CREAR", "Importar, copiar y agregar subcursos",
                "ACTUALIZAR", "Activar, desactivar y editar subcursos",
                "ELIMINAR", "Eliminar cursos y subcursos");
        // Las competencias vienen del Excel: solo se activan o desactivan.
        permiso("COMPETENCIAS",
                "ACTUALIZAR", "Activar y desactivar");
        permiso("ASIGNACIONES",
                "CREAR", "Asignar cursos",
                "ACTUALIZAR", "Editar asignaciones",
                "ELIMINAR", "Eliminar asignaciones");
        permiso("RECREOS",
                "CREAR", "Crear recreos",
                "ACTUALIZAR", "Editar recreos",
                "ELIMINAR", "Eliminar recreos");
    }

    private CatalogoPermisos() {
    }

    /** Pares accion, etiqueta en orden. */
    private static void permiso(String codigo, String... accionesYEtiquetas) {
        Map<String, String> acciones = new LinkedHashMap<>();
        for (int i = 0; i < accionesYEtiquetas.length; i += 2) {
            acciones.put(accionesYEtiquetas[i], accionesYEtiquetas[i + 1]);
        }
        CATALOGO.put(codigo, Map.copyOf(acciones));
    }

    /** Permisos que el catalogo controla. */
    public static Map<String, Map<String, String>> todos() {
        return CATALOGO;
    }

    /** Acciones (codigo -> etiqueta) del permiso, si el catalogo lo controla. */
    public static Optional<Map<String, String>> acciones(String codigoPermiso) {
        return Optional.ofNullable(CATALOGO.get(codigoPermiso));
    }
}
