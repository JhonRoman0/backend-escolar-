package com.example.Escolar.Security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RbacAuthorizationManagerTest {

    @Mock
    private ConcesionesService concesionesService;

    // Un rol "Portero" que solo tiene Docentes con editar, eliminar y reporte.
    private boolean puede(String metodo, String ruta) {
        when(concesionesService.accionesPorPermiso(9))
                .thenReturn(Map.of("DOCENTES", Set.of("ACTUALIZAR", "ELIMINAR", "IMPRIMIR_EXPORTAR")));
        RbacAuthorizationManager manager = new RbacAuthorizationManager(new ApiPermisoRegistro(), concesionesService);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(metodo);
        when(request.getRequestURI()).thenReturn(ruta);
        UsuarioAutenticado principal = new UsuarioAutenticado(9, "POR001", List.of("Portero"));
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
}
