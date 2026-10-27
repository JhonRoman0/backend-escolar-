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
            "MATRICULAS", "ASISTENCIAS", "NOTAS", "CONSOLIDADOS", "HORARIOS", "PLANTILLAS", "USUARIOS", "PAGOS_MATRICULA", "ORDEN_GRADOS"
    };

    private final List<Registro> registros = new ArrayList<>();
    private final AntPathMatcher matcher = new AntPathMatcher();

    public ApiPermisoRegistro() {
        registrarLectura("GET", "/usuarios", "USUARIOS");
        registrarLectura("GET", "/usuarios/**", "USUARIOS");
        registrar("POST", "/usuarios/{id}/foto", "USUARIOS", "ACTUALIZAR");
        registrar("DELETE", "/usuarios/{id}/foto", "USUARIOS", "ACTUALIZAR");
        registrar("POST", "/usuarios/{id}/desbloquear", "USUARIOS", "ACTUALIZAR");
        registrar("POST", "/usuarios/**", "USUARIOS", "CREAR");
        registrar("PUT", "/usuarios/**", "USUARIOS", "ACTUALIZAR");
        registrar("DELETE", "/usuarios/**", "USUARIOS", "ELIMINAR");
        registrar("GET", "/usuarios/reporte", "USUARIOS", "IMPRIMIR_EXPORTAR");

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

        registrarLectura("GET", "/docentes", "DOCENTES", "ASIGNACIONES", "SUSPENSIONES_DOCENTE", "HORARIOS", "NOTAS");
        registrarLectura("GET", "/docentes/**", "DOCENTES");
        registrarLectura("GET", "/grados-academicos", "DOCENTES", "USUARIOS");
        registrarLectura("GET", "/tipos-contrato", "DOCENTES", "USUARIOS");
        registrar("PUT", "/docentes/**", "DOCENTES", "ACTUALIZAR");
        registrar("DELETE", "/docentes/**", "DOCENTES", "ELIMINAR");
        registrar("GET", "/docentes/reporte", "DOCENTES", "IMPRIMIR_EXPORTAR");

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

        registrarLectura("GET", "/alumnos", "ALUMNOS");
        registrarLectura("GET", "/alumnos/**", "ALUMNOS");
        registrar("POST", "/alumnos/{id}/foto", "ALUMNOS", "ACTUALIZAR");
        registrar("DELETE", "/alumnos/{id}/foto", "ALUMNOS", "ACTUALIZAR");
        registrar("PUT", "/alumnos/**", "ALUMNOS", "ACTUALIZAR");
        registrar("DELETE", "/alumnos/**", "ALUMNOS", "ELIMINAR");
        registrar("GET", "/alumnos/reporte", "ALUMNOS", "IMPRIMIR_EXPORTAR");

        registrarLectura("GET", "/apoderados", "APODERADOS");
        registrarLectura("GET", "/apoderados/**", "APODERADOS");
        registrar("POST", "/apoderados", "APODERADOS", "CREAR");
        registrar("POST", "/apoderados/alumno/*", "APODERADOS", "CREAR");
        registrar("PUT", "/apoderados/**", "APODERADOS", "ACTUALIZAR");
        registrar("DELETE", "/apoderados/**", "APODERADOS", "ELIMINAR");

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
        registrarLectura("GET", "/matriculas", "MATRICULAS");
        registrarLectura("GET", "/matriculas/**", "MATRICULAS");
        registrar("POST", "/matriculas/inscripcion", "MATRICULAS", "CREAR");
        registrar("POST", "/matriculas/inscripcion/validar", "MATRICULAS", "CREAR");
        registrar("POST", "/matriculas/**", "MATRICULAS", "ACTUALIZAR");
        registrar("PUT", "/matriculas/**", "MATRICULAS", "ACTUALIZAR");
        registrar("DELETE", "/matriculas/**", "MATRICULAS", "ELIMINAR");
        registrar("GET", "/matriculas/reporte", "MATRICULAS", "IMPRIMIR_EXPORTAR");

        registrarLectura("GET", "/competencias", "COMPETENCIAS", "CURSOS", "NOTAS", "CONSOLIDADOS");
        registrarLectura("GET", "/competencias/**", "COMPETENCIAS", "CURSOS", "NOTAS", "CONSOLIDADOS");
        // Solo se activan o desactivan: se crean importando el Excel de cursos.
        registrar("PATCH", "/competencias/**", "COMPETENCIAS", "ACTUALIZAR");

        registrarLectura("GET", "/notas/competencia/**", "NOTAS");
        registrarLectura("GET", "/notas/matricula/**", "NOTAS");
        registrarLectura("GET", "/notas/consolidado", "CONSOLIDADOS");
        registrarLectura("GET", "/notas/exportar", "CONSOLIDADOS");
        registrar("POST", "/notas", "NOTAS", "CREAR");
        registrar("POST", "/notas/batch", "NOTAS", "CREAR");
        registrar("GET", "/notas/reporte", "NOTAS", "IMPRIMIR_EXPORTAR");
        registrar("POST", "/notas/autorizaciones", "NOTAS", "AUTORIZAR");
        registrarLectura("POST", "/notas/autorizaciones/validar", "NOTAS");
        registrarLectura("GET", "/notas/autorizaciones", "NOTAS");

        registrarLectura("GET", "/publicaciones", "PUBLICACIONES");
        registrarLectura("GET", "/publicaciones/**", "PUBLICACIONES");
        registrar("POST", "/publicaciones", "PUBLICACIONES", "CREAR");
        registrar("PUT", "/publicaciones/**", "PUBLICACIONES", "ACTUALIZAR");
        registrar("DELETE", "/publicaciones/**", "PUBLICACIONES", "ELIMINAR");
        registrar("POST", "/publicaciones/**/imagen", "PUBLICACIONES", "ACTUALIZAR");
        registrar("DELETE", "/publicaciones/**/imagen", "PUBLICACIONES", "ACTUALIZAR");

        registrarLectura("GET", "/eventos", "EVENTOS");
        registrarLectura("GET", "/eventos/**", "EVENTOS");
        registrar("POST", "/eventos", "EVENTOS", "CREAR");
        registrar("PUT", "/eventos/**", "EVENTOS", "ACTUALIZAR");
        registrar("DELETE", "/eventos/**", "EVENTOS", "ELIMINAR");
        registrar("POST", "/eventos/**/imagen", "EVENTOS", "ACTUALIZAR");
        registrar("DELETE", "/eventos/**/imagen", "EVENTOS", "ACTUALIZAR");

        registrarLectura("GET", "/galerias", "GALERIAS");
        registrarLectura("GET", "/galerias/**", "GALERIAS");
        registrar("POST", "/galerias", "GALERIAS", "CREAR");
        registrar("PUT", "/galerias/**", "GALERIAS", "ACTUALIZAR");
        registrar("DELETE", "/galerias/**", "GALERIAS", "ELIMINAR");
        registrar("POST", "/galerias/**/fotos", "GALERIAS", "ACTUALIZAR");
        registrar("DELETE", "/galerias/fotos/**", "GALERIAS", "ACTUALIZAR");

        registrarLectura("GET", "/contactos", "CONTACTOS");
        registrarLectura("GET", "/contactos/**", "CONTACTOS");
        registrar("PUT", "/contactos/**", "CONTACTOS", "ACTUALIZAR");

        registrarLectura("GET", "/ajustes", "AJUSTES");
        registrarLectura("GET", "/ajustes/**", "AJUSTES");
        registrar("POST", "/ajustes", "AJUSTES", "CREAR");
        registrar("PUT", "/ajustes/**", "AJUSTES", "ACTUALIZAR");
        registrar("DELETE", "/ajustes/**", "AJUSTES", "ELIMINAR");

        registrarLectura("GET", "/colegios", "COLEGIOS");
        registrarLectura("GET", "/colegios/**", "COLEGIOS");
        registrar("POST", "/colegios/*/foto", "COLEGIOS", "ACTUALIZAR");
        registrar("DELETE", "/colegios/*/foto", "COLEGIOS", "ACTUALIZAR");
        registrar("POST", "/colegios/*/portada", "COLEGIOS", "ACTUALIZAR");
        registrar("DELETE", "/colegios/*/portada", "COLEGIOS", "ACTUALIZAR");
        registrar("POST", "/colegios", "COLEGIOS", "CREAR");
        registrar("PUT", "/colegios/**", "COLEGIOS", "ACTUALIZAR");
        registrar("DELETE", "/colegios/**", "COLEGIOS", "ELIMINAR");

        registrarLectura("GET", "/asistencias", "ASISTENCIAS");
        registrarLectura("GET", "/asistencias/**", "ASISTENCIAS");
        registrar("POST", "/asistencias/**", "ASISTENCIAS", "CREAR");
        registrar("PUT", "/asistencias/**", "ASISTENCIAS", "ACTUALIZAR");
        registrar("DELETE", "/asistencias/**", "ASISTENCIAS", "ELIMINAR");

        registrarLectura("GET", "/reportes/**", "ASISTENCIAS");

        registrarLectura("GET", "/justificaciones", "ASISTENCIAS");
        registrarLectura("GET", "/justificaciones/**", "ASISTENCIAS");
        registrar("POST", "/justificaciones", "ASISTENCIAS", "CREAR");
        registrar("PUT", "/justificaciones/**", "ASISTENCIAS", "ACTUALIZAR");
        registrar("DELETE", "/justificaciones/**", "ASISTENCIAS", "ELIMINAR");

        registrarLectura("GET", "/dias-feriados", "ASISTENCIAS");
        registrarLectura("GET", "/dias-feriados/**", "ASISTENCIAS");
        registrar("POST", "/dias-feriados", "ASISTENCIAS", "CREAR");
        registrar("PUT", "/dias-feriados/**", "ASISTENCIAS", "ACTUALIZAR");
        registrar("DELETE", "/dias-feriados/**", "ASISTENCIAS", "ELIMINAR");

        registrarLectura("GET", "/horarios", "HORARIOS");
        registrarLectura("GET", "/horarios/**", "HORARIOS");

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
        registrar("PUT", "/suspensiones/**", "SUSPENSIONES_DOCENTE", "ACTUALIZAR");
        registrar("DELETE", "/suspensiones/**", "SUSPENSIONES_DOCENTE", "ELIMINAR");

        registrarLectura("GET", "/cambios-docente", "CAMBIOS_DOCENTE");
        registrarLectura("GET", "/cambios-docente/**", "CAMBIOS_DOCENTE");

        registrarLectura("GET", "/plantillas-siagie", "PLANTILLAS");
        registrarLectura("GET", "/plantillas-siagie/**", "PLANTILLAS");
        registrar("POST", "/plantillas-siagie", "PLANTILLAS", "CREAR");
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

    /** Acciones que piden las rutas de un permiso (sin contar las de solo lectura). */
    public Set<String> accionesDe(String permisoCodigo) {
        Set<String> acciones = new TreeSet<>();
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
