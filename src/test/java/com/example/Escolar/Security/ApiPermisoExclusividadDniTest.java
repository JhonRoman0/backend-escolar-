package com.example.Escolar.Security;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ApiPermisoExclusividadDniTest {
    @Test
    void validacionesNoExigenPermisosDeOtroModuloNiExponenCatalogos() {
        ApiPermisoRegistro registro = new ApiPermisoRegistro();
        var usuario = registro.buscar("GET", "/usuarios/validar-dni/12345678").orElseThrow();
        assertEquals(Set.of("USUARIOS"), usuario.permisosCodigo());
        assertNull(usuario.accionCodigo());
        var docente = registro.buscar("GET", "/docentes/validar-dni/12345678").orElseThrow();
        assertEquals(Set.of("DOCENTES"), docente.permisosCodigo());
        var alumno = registro.buscar("GET", "/matriculas/inscripcion/alumno/12345678").orElseThrow();
        assertEquals(Set.of("MATRICULAS"), alumno.permisosCodigo());
        var apoderado = registro.buscar("GET", "/matriculas/inscripcion/apoderado/12345678").orElseThrow();
        assertEquals(Set.of("MATRICULAS", "APODERADOS"), apoderado.permisosCodigo());
    }
}
