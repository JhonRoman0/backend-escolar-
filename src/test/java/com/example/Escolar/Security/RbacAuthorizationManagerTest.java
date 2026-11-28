package com.example.Escolar.Security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RbacAuthorizationManagerTest {

    @Mock
    private ConcesionesService concesionesService;

    // Un rol "Portero" que solo tiene Docentes con editar, eliminar y reporte.
    private boolean puede(String metodo, String ruta) {
        return puede(metodo, ruta, Map.of("DOCENTES", Set.of("ACTUALIZAR", "ELIMINAR", "IMPRIMIR_EXPORTAR")));
    }

    private boolean puede(String metodo, String ruta, Map<String, Set<String>> permisos) {
        when(concesionesService.accionesPorPermiso(9, 11))
                .thenReturn(permisos);
        RbacAuthorizationManager manager = new RbacAuthorizationManager(new ApiPermisoRegistro(), concesionesService);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(metodo);
        when(request.getRequestURI()).thenReturn(ruta);
        UsuarioAutenticado principal = new UsuarioAutenticado(9, "POR001", List.of("Portero"), null, 11);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        return manager.authorize(() -> auth, new RequestAuthorizationContext(request)).isGranted();
    }

    private boolean puede(String metodo, String ruta, UsuarioAutenticado principal) {
        RbacAuthorizationManager manager = new RbacAuthorizationManager(new ApiPermisoRegistro(), concesionesService);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(metodo);
        when(request.getRequestURI()).thenReturn(ruta);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        return manager.authorize(() -> auth, new RequestAuthorizationContext(request)).isGranted();
    }

    @Test
    void leeLosCatalogosQueUsaSuPantalla() {
        assertTrue(puede("GET", "/niveles"));
        assertTrue(puede("GET", "/anios-escolares"));
        assertTrue(puede("GET", "/grados-academicos"));
    }

    @Test
    void noEscribeEnLosCatalogos() {
        assertFalse(puede("POST", "/secciones"));
        assertFalse(puede("PUT", "/anios-escolares/1"));
    }

    @Test
    void noLeeCatalogosQueSuPantallaNoUsa() {
        assertFalse(puede("GET", "/aulas"));
        assertFalse(puede("GET", "/recreos"));
    }

    @Test
    void noLeeOtrosModulos() {
        assertFalse(puede("GET", "/alumnos"));
        assertFalse(puede("GET", "/asignaciones"));
    }

    @ParameterizedTest
    @CsvSource({
            "PATCH, /cursos/10/estado, ACTUALIZAR",
            "PATCH, /cursos/10/subcursos, ACTUALIZAR",
            "PUT, /cursos/20, ACTUALIZAR",
            "DELETE, /cursos/10, ELIMINAR",
            "POST, /cursos/10/subcursos, CREAR",
            "POST, /cursos/importar, CREAR",
            "POST, /cursos/importar/vista-previa, CREAR",
            "POST, /cursos/copiar, CREAR"
    })
    void cadaOperacionDeCursosExigeSuPermiso(String metodo, String ruta, String accion) {
        assertFalse(puede(metodo, ruta, Map.of()));
        assertFalse(puede(metodo, ruta, Map.of("CURSOS", Set.of())));
        assertFalse(puede(metodo, ruta, Map.of("DOCENTES", Set.of(accion))));
        for (String candidata : List.of("CREAR", "ACTUALIZAR", "ELIMINAR")) {
            boolean autorizado = puede(metodo, ruta, Map.of("CURSOS", Set.of(candidata)));
            if (candidata.equals(accion)) assertTrue(autorizado);
            else assertFalse(autorizado);
        }
    }

    @Test
    void sinIdRolActivoNiegaAunqueElTokenDigaAdmin() {
        UsuarioAutenticado principal = new UsuarioAutenticado(9, "ADM001", List.of("ADMIN"));
        when(concesionesService.accionesPorPermiso(9, 11)).thenReturn(Map.of("DOCENTES", Set.of("ACTUALIZAR")));

        assertFalse(puede("GET", "/alumnos", principal));
    }

    @Test
    void adminSoloSiElRolActivoEsAdmin() {
        when(concesionesService.esAdminActivo(9, 11)).thenReturn(true);
        UsuarioAutenticado admin = new UsuarioAutenticado(9, "ADM001", List.of("ADMIN"), null, 11);
        assertTrue(puede("GET", "/alumnos", admin));
        assertTrue(puede("POST", "/roles-permiso", admin));

        when(concesionesService.esAdminActivo(9, 12)).thenReturn(false);
        when(concesionesService.accionesPorPermiso(9, 12)).thenReturn(Map.of());
        UsuarioAutenticado otro = new UsuarioAutenticado(9, "POR001", List.of("ADMIN"), null, 12);
        assertFalse(puede("GET", "/alumnos", otro));
    }

    @Test
    void adminAsignadoPeroNoSeleccionadoNoHaceBypass() {
        when(concesionesService.esAdminActivo(9, 11)).thenReturn(false);
        when(concesionesService.esAdminActivo(9, 12)).thenReturn(true);
        when(concesionesService.accionesPorPermiso(9, 11))
                .thenReturn(Map.of("DOCENTES", Set.of("ACTUALIZAR")));
        UsuarioAutenticado principal = new UsuarioAutenticado(9, "USR001",
                List.of("ADMIN", "Docente"), null, 11);

        assertTrue(puede("GET", "/docentes", principal));
        assertFalse(puede("GET", "/alumnos", principal));
        verify(concesionesService, never()).esAdminActivo(eq(9), eq(12));
    }

    @Test
    void noUsaPermisosDeOtrosRoles() {
        when(concesionesService.accionesPorPermiso(9, 11)).thenReturn(Map.of());
        when(concesionesService.accionesPorPermiso(9, 12)).thenReturn(Map.of("DOCENTES", Set.of("ACTUALIZAR")));
        UsuarioAutenticado principal = new UsuarioAutenticado(9, "POR001", List.of("Docente"), null, 11);

        assertFalse(puede("GET", "/alumnos", principal));
    }

    @Test
    void noElevaPermisosUsandoOtroRolPropioParaConfigurarRolesPermiso() {
        when(concesionesService.accionesPorPermiso(9, 11))
                .thenReturn(Map.of("USUARIOS", Set.of("CREAR")));
        when(concesionesService.accionesPorPermiso(9, 12))
                .thenReturn(Map.of("ROLES_PERMISOS", Set.of("ACTUALIZAR")));
        UsuarioAutenticado principal = new UsuarioAutenticado(9, "POR001",
                List.of("Docente", "Portero"), null, 11);

        assertFalse(puede("POST", "/roles-permiso", principal));
    }
}
