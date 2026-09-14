package com.example.Escolar.Security;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ApiPermisoRegistro {

    public record Requisito(String permisoCodigo, String accionCodigo) {
    }

    private record Registro(String metodo, String patron, String permisoCodigo, String accionCodigo) {
    }

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

        registrarLectura("GET", "/docentes", "DOCENTES");
        registrarLectura("GET", "/docentes/**", "DOCENTES");
        registrar("POST", "/docentes", "DOCENTES", "CREAR");
        registrar("PUT", "/docentes/**", "DOCENTES", "ACTUALIZAR");
        registrar("DELETE", "/docentes/**", "DOCENTES", "ELIMINAR");
        registrar("GET", "/docentes/reporte", "DOCENTES", "IMPRIMIR_EXPORTAR");

        registrarLectura("GET", "/cursos", "CURSOS");
        registrarLectura("GET", "/cursos/**", "CURSOS");
        registrar("POST", "/cursos", "CURSOS", "CREAR");
        registrar("PUT", "/cursos/**", "CURSOS", "ACTUALIZAR");
        registrar("DELETE", "/cursos/**", "CURSOS", "ELIMINAR");

        registrarLectura("GET", "/turnos", "TURNOS");
        registrarLectura("GET", "/turnos/**", "TURNOS");
        registrar("POST", "/turnos", "TURNOS", "CREAR");
        registrar("PUT", "/turnos/**", "TURNOS", "ACTUALIZAR");
        registrar("DELETE", "/turnos/**", "TURNOS", "ELIMINAR");

        registrarLectura("GET", "/grados", "GRADOS");
        registrarLectura("GET", "/grados/**", "GRADOS");
        registrar("POST", "/grados", "GRADOS", "CREAR");
        registrar("PUT", "/grados/**", "GRADOS", "ACTUALIZAR");
        registrar("DELETE", "/grados/**", "GRADOS", "ELIMINAR");

        registrarLectura("GET", "/niveles", "GRADOS");
        registrarLectura("GET", "/secciones", "GRADOS");

        registrarLectura("GET", "/anios-escolares", "ANIOS_ESCOLARES");
        registrarLectura("GET", "/anios-escolares/**", "ANIOS_ESCOLARES");
        registrar("POST", "/anios-escolares", "ANIOS_ESCOLARES", "CREAR");
        registrar("PUT", "/anios-escolares/**", "ANIOS_ESCOLARES", "ACTUALIZAR");
        registrar("DELETE", "/anios-escolares/**", "ANIOS_ESCOLARES", "ELIMINAR");

        registrarLectura("GET", "/aulas", "AULAS");
        registrarLectura("GET", "/aulas/**", "AULAS");
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
        registrar("POST", "/alumnos", "ALUMNOS", "CREAR");
        registrar("PUT", "/alumnos/**", "ALUMNOS", "ACTUALIZAR");
        registrar("DELETE", "/alumnos/**", "ALUMNOS", "ELIMINAR");
        registrar("GET", "/alumnos/reporte", "ALUMNOS", "IMPRIMIR_EXPORTAR");

        registrarLectura("GET", "/apoderados", "APODERADOS");
        registrarLectura("GET", "/apoderados/**", "APODERADOS");
        registrar("POST", "/apoderados", "APODERADOS", "CREAR");
        registrar("PUT", "/apoderados/**", "APODERADOS", "ACTUALIZAR");
        registrar("DELETE", "/apoderados/**", "APODERADOS", "ELIMINAR");

        registrarLectura("GET", "/matriculas", "MATRICULAS");
        registrarLectura("GET", "/matriculas/**", "MATRICULAS");
        registrar("POST", "/matriculas", "MATRICULAS", "CREAR");
        registrar("POST", "/matriculas/**", "MATRICULAS", "ACTUALIZAR");
        registrar("PUT", "/matriculas/**", "MATRICULAS", "ACTUALIZAR");
        registrar("DELETE", "/matriculas/**", "MATRICULAS", "ELIMINAR");
        registrar("GET", "/matriculas/reporte", "MATRICULAS", "IMPRIMIR_EXPORTAR");

        registrarLectura("GET", "/competencias", "COMPETENCIAS");
        registrarLectura("GET", "/competencias/**", "COMPETENCIAS");
        registrar("POST", "/competencias", "COMPETENCIAS", "CREAR");
        registrar("PUT", "/competencias/**", "COMPETENCIAS", "ACTUALIZAR");
        registrar("DELETE", "/competencias/**", "COMPETENCIAS", "ELIMINAR");

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

        registrarLectura("GET", "/plantillas-siagie", "PLANTILLAS");
        registrarLectura("GET", "/plantillas-siagie/**", "PLANTILLAS");
        registrar("POST", "/plantillas-siagie", "PLANTILLAS", "CREAR");
    }

    private void registrar(String metodo, String patron, String permisoCodigo, String accionCodigo) {
        registros.add(new Registro(metodo, patron, permisoCodigo, accionCodigo));
    }

    private void registrarLectura(String metodo, String patron, String permisoCodigo) {
        registrar(metodo, patron, permisoCodigo, null);
    }

    public Optional<Requisito> buscar(String metodo, String path) {
        for (Registro registro : registros) {
            if (registro.metodo().equals(metodo) && matcher.match(registro.patron(), path)) {
                return Optional.of(new Requisito(registro.permisoCodigo(), registro.accionCodigo()));
            }
        }
        return Optional.empty();
    }
}
