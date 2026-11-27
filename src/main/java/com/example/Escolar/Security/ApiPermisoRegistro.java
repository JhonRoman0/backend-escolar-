package com.example.Escolar.Security;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Component
public class ApiPermisoRegistro {

    // Basta con tener uno de los permisos (más de uno solo en lecturas de catálogo).
    public record Requisito(Set<String> permisosCodigo, String accionCodigo) {
    }

    private record Registro(String metodo, String patron, Set<String> permisosCodigo, String accionCodigo) {
    }

    // Módulos que muestran filtros o selectores de la estructura académica (niveles, años, grados, secciones, turnos).
    private static final String[] LECTORES_ESTRUCTURA = {
            "DOCENTES", "CURSOS", "COMPETENCIAS", "TURNOS", "GRADOS", "ANIOS_ESCOLARES", "AULAS", "ASIGNACIONES",
            "RECREOS", "CONFIGURACION_ACADEMICA", "SUSPENSIONES_DOCENTE", "CAMBIOS_DOCENTE", "ALUMNOS", "APODERADOS",
            "MATRICULAS", "ASISTENCIAS", "NOTAS", "HORARIOS", "EVALUACIONES", "USUARIOS", "PAGOS_MATRICULA", "ORDEN_GRADOS"
    };

    private final List<Registro> registros = new ArrayList<>();
    /** Acciones que se cumplen en la pantalla, sin ruta propia (permiso → acciones). */
    private final java.util.Map<String, Set<String>> accionesDePantalla = new java.util.HashMap<>();
    private final AntPathMatcher matcher = new AntPathMatcher();

    public ApiPermisoRegistro() {
        // Los reportes van antes que la lectura general: gana la primera ruta que coincide.
        registrar("GET", "/usuarios/reporte", "USUARIOS", "IMPRIMIR_EXPORTAR");
        registrarLectura("GET", "/usuarios", "USUARIOS");
        registrarLectura("GET", "/usuarios/**", "USUARIOS");
        registrar("POST", "/usuarios/{id}/foto", "USUARIOS", "ACTUALIZAR");
        registrar("DELETE", "/usuarios/{id}/foto", "USUARIOS", "ACTUALIZAR");
        registrar("POST", "/usuarios/{id}/desbloquear", "USUARIOS", "ACTUALIZAR");
        registrar("POST", "/usuarios/**", "USUARIOS", "CREAR");
        registrar("PUT", "/usuarios/**", "USUARIOS", "ACTUALIZAR");
        registrar("DELETE", "/usuarios/**", "USUARIOS", "ELIMINAR");

        registrarLectura("GET", "/roles", "ROLES");
        registrarLectura("GET", "/roles/**", "ROLES");
        registrar("POST", "/roles", "ROLES", "CREAR");
        registrar("PUT", "/roles/**", "ROLES", "ACTUALIZAR");
        registrar("DELETE", "/roles/**", "ROLES", "ELIMINAR");

        registrarLectura("GET", "/modulos", "MODULOS");
        registrarLectura("GET", "/modulos/**", "MODULOS");
        registrar("POST", "/modulos", "MODULOS", "CREAR");
        registrar("PUT", "/modulos/**", "MODULOS", "ACTUALIZAR");
        registrar("DELETE", "/modulos/**", "MODULOS", "ELIMINAR");

        registrarLectura("GET", "/permisos", "PERMISOS");
        registrarLectura("GET", "/permisos/**", "PERMISOS");
        registrar("POST", "/permisos", "PERMISOS", "CREAR");
        registrar("PUT", "/permisos/**", "PERMISOS", "ACTUALIZAR");
        registrar("DELETE", "/permisos/**", "PERMISOS", "ELIMINAR");

        registrarLectura("GET", "/acciones", "ACCIONES");
        registrarLectura("GET", "/acciones/**", "ACCIONES");
        registrar("POST", "/acciones", "ACCIONES", "CREAR");
        registrar("PUT", "/acciones/**", "ACCIONES", "ACTUALIZAR");
        registrar("DELETE", "/acciones/**", "ACCIONES", "ELIMINAR");

        registrarLectura("GET", "/roles-permiso", "ROLES_PERMISOS");
        registrarLectura("GET", "/roles-permiso/**", "ROLES_PERMISOS");
        registrar("POST", "/roles-permiso", "ROLES_PERMISOS", "CREAR");
        registrar("PUT", "/roles-permiso/**", "ROLES_PERMISOS", "ACTUALIZAR");
        registrar("DELETE", "/roles-permiso/**", "ROLES_PERMISOS", "ELIMINAR");

        registrar("GET", "/docentes/reporte", "DOCENTES", "IMPRIMIR_EXPORTAR");
        registrarLectura("GET", "/docentes", "DOCENTES", "ASIGNACIONES", "SUSPENSIONES_DOCENTE", "HORARIOS", "NOTAS", "CAMBIOS_DOCENTE");
        registrarLectura("GET", "/docentes/**", "DOCENTES");
        registrarLectura("GET", "/grados-academicos", "DOCENTES", "USUARIOS");
        registrarLectura("GET", "/tipos-contrato", "DOCENTES", "USUARIOS");
        registrar("PUT", "/docentes/**", "DOCENTES", "ACTUALIZAR");
        registrar("DELETE", "/docentes/**", "DOCENTES", "ELIMINAR");

        registrarLectura("GET", "/cursos", "CURSOS", "COMPETENCIAS", "ASIGNACIONES");
        registrarLectura("GET", "/cursos/**", "CURSOS", "COMPETENCIAS", "ASIGNACIONES");
        // Importar, vista previa, copiar y crear subcursos.
        registrar("POST", "/cursos/**", "CURSOS", "CREAR");
        registrar("PUT", "/cursos/**", "CURSOS", "ACTUALIZAR");
        registrar("PATCH", "/cursos/**", "CURSOS", "ACTUALIZAR");
        registrar("DELETE", "/cursos/**", "CURSOS", "ELIMINAR");

        registrarLectura("GET", "/turnos", "TURNOS", LECTORES_ESTRUCTURA);
        registrarLectura("GET", "/turnos/**", "TURNOS", LECTORES_ESTRUCTURA);
        registrar("POST", "/turnos", "TURNOS", "CREAR");
        registrar("PUT", "/turnos/**", "TURNOS", "ACTUALIZAR");
        registrar("DELETE", "/turnos/**", "TURNOS", "ELIMINAR");

        registrarLectura("GET", "/grados", "GRADOS", LECTORES_ESTRUCTURA);
        registrarLectura("GET", "/grados/**", "GRADOS", LECTORES_ESTRUCTURA);
        registrar("POST", "/grados", "GRADOS", "CREAR");
        // Antes que PUT /grados/**: el orden tiene permiso propio.
        registrar("PUT", "/grados/*/orden", "ORDEN_GRADOS", "ACTUALIZAR");
        registrar("PATCH", "/grados/*/repite", "ORDEN_GRADOS", "ACTUALIZAR");
        registrar("PUT", "/grados/**", "GRADOS", "ACTUALIZAR");
        registrar("DELETE", "/grados/**", "GRADOS", "ELIMINAR");

        registrarLectura("GET", "/niveles", "GRADOS", LECTORES_ESTRUCTURA);
        registrar("PUT", "/niveles/*/orden", "ORDEN_GRADOS", "ACTUALIZAR");
        registrarLectura("GET", "/secciones", "GRADOS", LECTORES_ESTRUCTURA);
        registrarLectura("GET", "/secciones/**", "GRADOS", LECTORES_ESTRUCTURA);
        registrar("POST", "/secciones", "GRADOS", "CREAR");
        registrar("POST", "/secciones/lote", "GRADOS", "CREAR");
        registrar("PUT", "/secciones/**", "GRADOS", "ACTUALIZAR");
        registrar("DELETE", "/secciones/**", "GRADOS", "ELIMINAR");

        registrarLectura("GET", "/anios-escolares", "ANIOS_ESCOLARES", LECTORES_ESTRUCTURA);
        registrarLectura("GET", "/anios-escolares/**", "ANIOS_ESCOLARES", LECTORES_ESTRUCTURA);
        registrar("POST", "/anios-escolares", "ANIOS_ESCOLARES", "CREAR");
        registrar("PUT", "/anios-escolares/**", "ANIOS_ESCOLARES", "ACTUALIZAR");
        registrar("PATCH", "/anios-escolares/**", "ANIOS_ESCOLARES", "ACTUALIZAR");
        registrar("DELETE", "/anios-escolares/**", "ANIOS_ESCOLARES", "ELIMINAR");

        registrarLectura("GET", "/aulas", "AULAS", "GRADOS", "ASIGNACIONES");
        registrarLectura("GET", "/aulas/**", "AULAS", "GRADOS", "ASIGNACIONES");
        registrar("POST", "/aulas", "AULAS", "CREAR");
        registrar("PUT", "/aulas/**", "AULAS", "ACTUALIZAR");
        registrar("DELETE", "/aulas/**", "AULAS", "ELIMINAR");

        registrarLectura("GET", "/asignaciones", "ASIGNACIONES");
        registrarLectura("GET", "/asignaciones/**", "ASIGNACIONES");
        registrar("POST", "/asignaciones", "ASIGNACIONES", "CREAR");
        registrar("PUT", "/asignaciones/**", "ASIGNACIONES", "ACTUALIZAR");
        registrar("DELETE", "/asignaciones/**", "ASIGNACIONES", "ELIMINAR");

        // Los apoderados se gestionan desde la lista de alumnos.
        registrar("GET", "/alumnos/reporte", "ALUMNOS", "IMPRIMIR_EXPORTAR");
        registrarLectura("GET", "/alumnos", "ALUMNOS", "APODERADOS");
        registrarLectura("GET", "/alumnos/**", "ALUMNOS");
        registrar("POST", "/alumnos/{id}/foto", "ALUMNOS", "ACTUALIZAR");
        registrar("DELETE", "/alumnos/{id}/foto", "ALUMNOS", "ACTUALIZAR");
        registrar("PUT", "/alumnos/**", "ALUMNOS", "ACTUALIZAR");

        registrarLectura("GET", "/apoderados", "APODERADOS");
        registrarLectura("GET", "/apoderados/**", "APODERADOS");
        registrar("POST", "/apoderados/alumno/*", "APODERADOS", "CREAR");
        registrar("PUT", "/apoderados/**", "APODERADOS", "ACTUALIZAR");
        registrar("DELETE", "/apoderados/*", "APODERADOS", "ELIMINAR");

        registrarLectura("GET", "/parentescos", "MATRICULAS", "APODERADOS", "ALUMNOS");
        registrarLectura("GET", "/sexos", "MATRICULAS", "APODERADOS", "ALUMNOS");

        registrarLectura("GET", "/metodos-pago", "PAGOS_MATRICULA", "MATRICULAS");
        registrar("POST", "/metodos-pago", "PAGOS_MATRICULA", "CREAR");
        registrar("PUT", "/metodos-pago/*", "PAGOS_MATRICULA", "ACTUALIZAR");
        registrar("DELETE", "/metodos-pago/*", "PAGOS_MATRICULA", "ELIMINAR");
        registrarLectura("GET", "/montos-matricula", "PAGOS_MATRICULA", "MATRICULAS");
        registrar("PUT", "/montos-matricula", "PAGOS_MATRICULA", "ACTUALIZAR");

        // Estudiantes también busca apoderados por DNI al agregar uno.
        registrarLectura("GET", "/matriculas/inscripcion/apoderado/*", "MATRICULAS", "APODERADOS");
        registrar("GET", "/matriculas/reporte", "MATRICULAS", "IMPRIMIR_EXPORTAR");
        registrar("GET", "/matriculas/{id}/ficha", "MATRICULAS", "IMPRIMIR_FICHA");
        registrarLectura("GET", "/matriculas", "MATRICULAS");
        registrarLectura("GET", "/matriculas/**", "MATRICULAS");
        registrar("POST", "/matriculas/inscripcion", "MATRICULAS", "CREAR");
        registrar("POST", "/matriculas/inscripcion/validar", "MATRICULAS", "CREAR");
        registrar("POST", "/matriculas/**", "MATRICULAS", "ACTUALIZAR");
        registrar("PUT", "/matriculas/**", "MATRICULAS", "ACTUALIZAR");
        registrar("DELETE", "/matriculas/**", "MATRICULAS", "ELIMINAR");

        // Registrar notas de una evaluación es parte de crearla; editar sus datos es otra acción.
        registrarLectura("GET", "/evaluaciones/historial-calificaciones", "EVALUACIONES");
        registrar("POST", "/evaluaciones/reconocimientos", "EVALUACIONES", "CREAR");
        registrar("POST", "/evaluaciones/reconocimientos/*/anulacion", "EVALUACIONES", "ACTUALIZAR");
        registrar("GET", "/evaluaciones/*/resumen", "EVALUACIONES", "VER_RESUMEN");
        registrarLectura("GET", "/evaluaciones", "EVALUACIONES");
        registrarLectura("GET", "/evaluaciones/**", "EVALUACIONES");
        registrar("POST", "/evaluaciones", "EVALUACIONES", "CREAR");
        registrar("PUT", "/evaluaciones/*/notas", "EVALUACIONES", "CREAR");
        registrar("PUT", "/evaluaciones/**", "EVALUACIONES", "ACTUALIZAR");
        registrar("DELETE", "/evaluaciones/**", "EVALUACIONES", "ELIMINAR");
        registrarDePantalla("EVALUACIONES", "IMPRIMIR_EXPORTAR");

        registrarLectura("GET", "/competencias", "COMPETENCIAS", "CURSOS", "NOTAS");
        registrarLectura("GET", "/competencias/**", "COMPETENCIAS", "CURSOS", "NOTAS");
        // Solo se activan o desactivan: se crean importando el Excel de cursos.
        registrar("PATCH", "/competencias/**", "COMPETENCIAS", "ACTUALIZAR");

        registrar("GET", "/notas/boletas", "NOTAS", "IMPRIMIR_BOLETA");
        registrar("GET", "/notas/siagie", "NOTAS", "EXPORTAR_SIAGIE");
        registrar("GET", "/notas/siagie/**", "NOTAS", "EXPORTAR_SIAGIE");
        registrarLectura("GET", "/notas/finales", "NOTAS");
        registrarLectura("GET", "/notas/finales/**", "NOTAS");
        registrar("PUT", "/notas/finales/correccion", "NOTAS", "ACTUALIZAR");
        registrar("POST", "/notas/finales/reapertura", "NOTAS", "ACTUALIZAR");
        registrarLectura("GET", "/notas/anuales", "NOTAS_ANUALES");
        registrarLectura("GET", "/notas/anuales/**", "NOTAS_ANUALES");
        registrar("PUT", "/notas/anuales/correccion", "NOTAS_ANUALES", "ACTUALIZAR");
        registrar("POST", "/notas/anuales/reapertura", "NOTAS_ANUALES", "ACTUALIZAR");
        registrar("PUT", "/notas/anuales", "NOTAS_ANUALES", "CREAR");
        registrar("PUT", "/notas/finales", "NOTAS", "CREAR");
        registrarLectura("GET", "/notas/competencia/**", "NOTAS");
        registrarLectura("GET", "/notas/matricula/**", "NOTAS");
        registrar("GET", "/notas/reporte", "NOTAS", "IMPRIMIR_EXPORTAR");

        // Portal del apoderado: el servicio limita cada consulta a sus hijos.
        registrarLectura("GET", "/portal-apoderado/**", "PORTAL_APODERADO");

        registrarLectura("GET", "/colegios", "COLEGIOS");
        registrarLectura("GET", "/colegios/**", "COLEGIOS");
        registrar("POST", "/colegios/*/foto", "COLEGIOS", "ACTUALIZAR");
        registrar("POST", "/colegios/*/portada", "COLEGIOS", "ACTUALIZAR");
        registrar("DELETE", "/colegios/*/portada", "COLEGIOS", "ACTUALIZAR");
        registrar("POST", "/colegios", "COLEGIOS", "CREAR");
        registrar("PUT", "/colegios/**", "COLEGIOS", "ACTUALIZAR");
        registrar("DELETE", "/colegios/**", "COLEGIOS", "ELIMINAR");

        registrarLectura("GET", "/asistencias", "ASISTENCIAS");
        registrarLectura("GET", "/asistencias/**", "ASISTENCIAS");
        // Anular va antes que el registro general (gana la primera ruta que coincide).
        registrar("POST", "/asistencias/*/anulacion", "ASISTENCIAS", "ELIMINAR");
        registrar("POST", "/asistencias/**", "ASISTENCIAS", "CREAR");
        registrar("PUT", "/asistencias/**", "ASISTENCIAS", "ACTUALIZAR");

        registrar("GET", "/reportes/**", "ASISTENCIAS", "IMPRIMIR_EXPORTAR");

        registrarLectura("GET", "/justificaciones", "ASISTENCIAS");
        registrarLectura("GET", "/justificaciones/**", "ASISTENCIAS");
        registrar("POST", "/justificaciones", "ASISTENCIAS", "CONFIGURAR");
        registrar("PUT", "/justificaciones/**", "ASISTENCIAS", "CONFIGURAR");
        registrar("DELETE", "/justificaciones/**", "ASISTENCIAS", "CONFIGURAR");

        registrarLectura("GET", "/dias-feriados", "ASISTENCIAS");
        registrarLectura("GET", "/dias-feriados/**", "ASISTENCIAS");
        registrar("POST", "/dias-feriados", "ASISTENCIAS", "CONFIGURAR");
        registrar("PUT", "/dias-feriados/**", "ASISTENCIAS", "CONFIGURAR");
        registrar("DELETE", "/dias-feriados/**", "ASISTENCIAS", "CONFIGURAR");

        // El horario del colegio pide VER_COLEGIO; el propio y el del hijo solo la lectura.
        registrar("GET", "/horarios", "HORARIOS", "VER_COLEGIO");
        registrar("GET", "/horarios/seccion/*/recreos", "HORARIOS", "VER_COLEGIO");
        registrarLectura("GET", "/horarios/**", "HORARIOS");
        // El PDF del horario se arma en el navegador con lo ya cargado.
        registrarDePantalla("HORARIOS", "IMPRIMIR_EXPORTAR");

        registrarLectura("GET", "/configuracion-academica", "CONFIGURACION_ACADEMICA");
        registrar("PUT", "/configuracion-academica", "CONFIGURACION_ACADEMICA", "ACTUALIZAR");

        registrarLectura("GET", "/recreos", "RECREOS", "ASIGNACIONES");
        registrarLectura("GET", "/recreos/**", "RECREOS", "ASIGNACIONES");
        registrar("POST", "/recreos", "RECREOS", "CREAR");
        registrar("PUT", "/recreos/**", "RECREOS", "ACTUALIZAR");
        registrar("DELETE", "/recreos/**", "RECREOS", "ELIMINAR");

        registrarLectura("GET", "/suspensiones", "SUSPENSIONES_DOCENTE");
        registrarLectura("GET", "/suspensiones/**", "SUSPENSIONES_DOCENTE");
        registrar("POST", "/suspensiones", "SUSPENSIONES_DOCENTE", "CREAR");
        registrar("POST", "/suspensiones/*/cancelar", "SUSPENSIONES_DOCENTE", "ACTUALIZAR");
        registrar("POST", "/suspensiones/*/finalizar", "SUSPENSIONES_DOCENTE", "ACTUALIZAR");
        registrar("PUT", "/suspensiones/*/edicion-vigente", "SUSPENSIONES_DOCENTE", "ACTUALIZAR");
        registrar("PUT", "/suspensiones/**", "SUSPENSIONES_DOCENTE", "ACTUALIZAR");
        registrar("DELETE", "/suspensiones/**", "SUSPENSIONES_DOCENTE", "ELIMINAR");

        registrarLectura("GET", "/cambios-docente", "CAMBIOS_DOCENTE");
        registrarLectura("GET", "/cambios-docente/**", "CAMBIOS_DOCENTE");
        registrar("POST", "/cambios-docente", "CAMBIOS_DOCENTE", "CREAR");
        registrar("PUT", "/cambios-docente/*/edicion-vigente", "CAMBIOS_DOCENTE", "ACTUALIZAR");
        registrar("PUT", "/cambios-docente/*/asignar-sustituto", "CAMBIOS_DOCENTE", "ACTUALIZAR");
        registrar("PUT", "/cambios-docente/*/resolver-incidencia", "CAMBIOS_DOCENTE", "ACTUALIZAR");
        registrar("PUT", "/cambios-docente/*/finalizar", "CAMBIOS_DOCENTE", "ACTUALIZAR");
        registrar("PUT", "/cambios-docente/*/cancelar", "CAMBIOS_DOCENTE", "ACTUALIZAR");
        registrar("PUT", "/cambios-docente/*", "CAMBIOS_DOCENTE", "ACTUALIZAR");

        registrarLectura("GET", "/motivos-cambio-docente", "CAMBIOS_DOCENTE");
        registrarLectura("GET", "/motivos-cambio-docente/**", "CAMBIOS_DOCENTE");
        registrar("POST", "/motivos-cambio-docente", "CAMBIOS_DOCENTE", "CONFIGURAR");
        registrar("PUT", "/motivos-cambio-docente/**", "CAMBIOS_DOCENTE", "CONFIGURAR");
        registrar("DELETE", "/motivos-cambio-docente/**", "CAMBIOS_DOCENTE", "CONFIGURAR");

    }

    private void registrar(String metodo, String patron, String permisoCodigo, String accionCodigo) {
        registros.add(new Registro(metodo, patron, Set.of(permisoCodigo), accionCodigo));
    }

    // Los `lectores` son otros permisos que también pueden leer la ruta porque sus pantallas la usan.
    private void registrarLectura(String metodo, String patron, String permisoCodigo, String... lectores) {
        Set<String> permisos = new LinkedHashSet<>();
        permisos.add(permisoCodigo);
        permisos.addAll(List.of(lectores));
        registros.add(new Registro(metodo, patron, Set.copyOf(permisos), null));
    }

    private void registrarDePantalla(String permisoCodigo, String accionCodigo) {
        accionesDePantalla.computeIfAbsent(permisoCodigo, k -> new TreeSet<>()).add(accionCodigo);
    }

    /** Acciones que piden las rutas de un permiso (sin contar las de solo lectura) o su pantalla. */
    public Set<String> accionesDe(String permisoCodigo) {
        Set<String> acciones = new TreeSet<>(accionesDePantalla.getOrDefault(permisoCodigo, Set.of()));
        for (Registro registro : registros) {
            if (registro.permisosCodigo().contains(permisoCodigo) && registro.accionCodigo() != null) {
                acciones.add(registro.accionCodigo());
            }
        }
        return acciones;
    }

    public Optional<Requisito> buscar(String metodo, String path) {
        for (Registro registro : registros) {
            if (registro.metodo().equals(metodo) && matcher.match(registro.patron(), path)) {
                return Optional.of(new Requisito(registro.permisosCodigo(), registro.accionCodigo()));
            }
        }
        return Optional.empty();
    }
}
