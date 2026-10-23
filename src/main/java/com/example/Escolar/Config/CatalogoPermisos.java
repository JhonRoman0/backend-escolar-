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

    private static final String SEGURIDAD = "Seguridad";
    private static final String ACADEMICO = "Académico";

    private static final Map<String, Map<String, String>> CATALOGO = new LinkedHashMap<>();
    private static final Map<String, String> MODULOS = new LinkedHashMap<>();

    static {
        permiso("USUARIOS", SEGURIDAD,
                "CREAR", "Crear usuarios",
                "ACTUALIZAR", "Editar, cambiar foto y desbloquear",
                "ELIMINAR", "Eliminar usuarios",
                "IMPRIMIR_EXPORTAR", "Exportar reporte");
        permiso("ROLES", SEGURIDAD,
                "CREAR", "Crear roles",
                "ACTUALIZAR", "Editar roles",
                "ELIMINAR", "Eliminar roles");
        permiso("TURNOS", ACADEMICO,
                "CREAR", "Crear turnos",
                "ACTUALIZAR", "Editar horarios del turno",
                "ELIMINAR", "Eliminar turnos");
        permiso("AULAS", ACADEMICO,
                "CREAR", "Crear aulas y definir su tipo",
                "ACTUALIZAR", "Editar aulas y su tipo",
                "ELIMINAR", "Eliminar aulas");
        permiso("ANIOS_ESCOLARES", ACADEMICO,
                "CREAR", "Crear años escolares",
                "ACTUALIZAR", "Editar fechas y cambiar el estado",
                "ELIMINAR", "Eliminar años escolares");
        permiso("GRADOS", ACADEMICO,
                "CREAR", "Crear grados y secciones",
                "ACTUALIZAR", "Editar grados y secciones",
                "ELIMINAR", "Eliminar grados y secciones");
        // Los docentes se crean desde Usuarios, al asignar el rol Docente.
        permiso("DOCENTES", ACADEMICO,
                "ACTUALIZAR", "Editar datos y foto",
                "ELIMINAR", "Eliminar docentes",
                "IMPRIMIR_EXPORTAR", "Exportar reporte");
        permiso("CURSOS", ACADEMICO,
                "CREAR", "Importar, copiar y agregar subcursos",
                "ACTUALIZAR", "Activar, desactivar y editar subcursos",
                "ELIMINAR", "Eliminar cursos y subcursos");
        // Las competencias vienen del Excel: solo se activan o desactivan.
        permiso("COMPETENCIAS", ACADEMICO,
                "ACTUALIZAR", "Activar y desactivar");
        permiso("ASIGNACIONES", ACADEMICO,
                "CREAR", "Asignar cursos",
                "ACTUALIZAR", "Editar asignaciones",
                "ELIMINAR", "Eliminar asignaciones");
        permiso("CONFIGURACION_ACADEMICA", ACADEMICO,
                "ACTUALIZAR", "Editar la hora académica");
        permiso("RECREOS", ACADEMICO,
                "CREAR", "Crear recreos",
                "ACTUALIZAR", "Editar recreos",
                "ELIMINAR", "Eliminar recreos");
    }

    private CatalogoPermisos() {
    }

    /** Modulo al que pertenece el permiso y pares accion, etiqueta en orden. */
    private static void permiso(String codigo, String modulo, String... accionesYEtiquetas) {
        MODULOS.put(codigo, modulo);
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

    /** Modulo (por nombre) en el que la pantalla de accesos muestra el permiso. */
    public static String modulo(String codigoPermiso) {
        return MODULOS.get(codigoPermiso);
    }

    /** Acciones (codigo -> etiqueta) del permiso, si el catalogo lo controla. */
    public static Optional<Map<String, String>> acciones(String codigoPermiso) {
        return Optional.ofNullable(CATALOGO.get(codigoPermiso));
    }
}
