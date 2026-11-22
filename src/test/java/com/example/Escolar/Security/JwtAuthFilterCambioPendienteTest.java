package com.example.Escolar.Security;

import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Repository.UsuarioVigenciaSesion;
import com.example.Escolar.Service.AccesoConstants;
import com.example.Escolar.Service.VigenciaService;
import com.example.Escolar.Util.FechaColegio;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthFilterCambioPendienteTest {

    private final JwtService jwtService = new JwtService("clave-secreta-de-pruebas-con-32-bytes-o-mas!!", 3_600_000);
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ObjectProvider<UsuarioRepository> usuarioRepositoryProvider;
    private JwtAuthFilter filter;

    @BeforeEach
    void prepararSesion() {
        when(usuarioRepositoryProvider.getObject()).thenReturn(usuarioRepository);
        filter = new JwtAuthFilter(jwtService, usuarioRepositoryProvider, new VigenciaService());
        when(usuarioRepository.findVigenciaSesion(eq(1), eq(AccesoConstants.ELIMINADO)))
                .thenReturn(Optional.of(sesionValida(0)));
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private UsuarioVigenciaSesion sesionValida(Integer versionToken) {
        LocalDate inicio = FechaColegio.hoy().minusDays(1);
        return new UsuarioVigenciaSesion() {
            @Override
            public Integer getIdUsuario() {
                return 1;
            }

            @Override
            public LocalDate getFechaInicioVigencia() {
                return inicio;
            }

            @Override
            public LocalDate getFechaFinVigencia() {
                return null;
            }

            @Override
            public Integer getVersionToken() {
                return versionToken;
            }

            @Override
            public Long getAccesoId() {
                return AccesoConstants.ACTIVO;
            }
        };
    }

    private MockHttpServletResponse ejecutar(String metodo, String ruta, boolean cambioPendiente, MockFilterChain chain)
            throws Exception {
        String token = jwtService.generarToken(1, "ADM20260001", List.of("DOCENTE"), cambioPendiente);
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, ruta);
        request.setServletPath(ruta);
        request.setCookies(new Cookie("escolar_token", token));
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test
    void conCambioPendienteBloqueaRutasDelSistema() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletResponse response = ejecutar("GET", "/usuarios", true, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains(JwtAuthFilter.CODIGO_CAMBIO_PENDIENTE));
        assertNull(chain.getRequest());
    }

    @Test
    void conCambioPendientePermiteCambiarContrasenaYConsultarSesion() throws Exception {
        for (String ruta : List.of("/auth/change-password", "/auth/me", "/auth/permisos", "/auth/logout")) {
            MockFilterChain chain = new MockFilterChain();
            String metodo = ruta.equals("/auth/me") || ruta.equals("/auth/permisos") ? "GET" : "POST";

            MockHttpServletResponse response = ejecutar(metodo, ruta, true, chain);

            assertEquals(200, response.getStatus(), ruta);
            assertNotNull(chain.getRequest(), ruta);
        }
    }

    @Test
    void sinCambioPendienteNoBloquea() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletResponse response = ejecutar("GET", "/usuarios", false, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    @Test
    void tokenAntiguoSinMarcaNoBloquea() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        String token = jwtService.generarToken(1, "ADM20260001", List.of("DOCENTE"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/usuarios");
        request.setServletPath("/usuarios");
        request.setCookies(new Cookie("escolar_token", token));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }
}
