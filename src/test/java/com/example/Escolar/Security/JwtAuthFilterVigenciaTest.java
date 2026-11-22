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
class JwtAuthFilterVigenciaTest {

    private final JwtService jwtService = new JwtService("clave-secreta-de-pruebas-con-32-bytes-o-mas!!", 3_600_000);
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ObjectProvider<UsuarioRepository> usuarioRepositoryProvider;
    private JwtAuthFilter filter;

    @BeforeEach
    void prepararFiltro() {
        when(usuarioRepositoryProvider.getObject()).thenReturn(usuarioRepository);
        filter = new JwtAuthFilter(jwtService, usuarioRepositoryProvider, new VigenciaService());
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private UsuarioVigenciaSesion sesion(LocalDate inicio, LocalDate fin, Integer versionToken) {
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
                return fin;
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

    private void conSesion(UsuarioVigenciaSesion sesion) {
        when(usuarioRepository.findVigenciaSesion(eq(1), eq(AccesoConstants.ELIMINADO)))
                .thenReturn(Optional.of(sesion));
    }

    private MockHttpServletResponse ejecutar(String ruta, String token, MockFilterChain chain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", ruta);
        request.setServletPath(ruta);
        request.setCookies(new Cookie("escolar_token", token));
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test
    void vigenciaVencidaBloqueaRutaCon403() throws Exception {
        LocalDate inicio = FechaColegio.hoy().minusDays(30);
        LocalDate fin = FechaColegio.hoy().minusDays(1);
        conSesion(sesion(inicio, fin, 0));
        String token = jwtService.generarToken(1, "ADM20240001", List.of("ADMIN"));
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletResponse response = ejecutar("/usuarios", token, chain);

        assertEquals(403, response.getStatus());
        String cuerpo = response.getContentAsString();
        assertTrue(cuerpo.contains("VIGENCIA_VENCIDA"), cuerpo);
        assertTrue(cuerpo.contains(inicio.toString()), cuerpo);
        assertTrue(cuerpo.contains(fin.toString()), cuerpo);
        assertTrue(cuerpo.contains("\"message\""), cuerpo);
        assertNull(chain.getRequest());
    }

    @Test
    void vigenciaVencidaPermiteSoloMeYLogout() throws Exception {
        LocalDate inicio = FechaColegio.hoy().minusDays(30);
        LocalDate fin = FechaColegio.hoy().minusDays(1);
        conSesion(sesion(inicio, fin, 0));
        String token = jwtService.generarToken(1, "ADM20240001", List.of("ADMIN"));

        for (String ruta : List.of("/auth/me", "/auth/logout")) {
            MockFilterChain chain = new MockFilterChain();

            MockHttpServletResponse response = ejecutar(ruta, token, chain);

            assertEquals(200, response.getStatus(), ruta);
            assertNotNull(chain.getRequest(), ruta);
        }
    }

    @Test
    void versionObsoletaBloqueaLaSesion() throws Exception {
        conSesion(sesion(FechaColegio.hoy().minusDays(1), null, 2));
        String token = jwtService.generarToken(1, "ADM20240001", List.of("ADMIN"), false, null, 1);
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletResponse response = ejecutar("/usuarios", token, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("VIGENCIA_TOKEN_OBSOLETO"));
        assertNull(chain.getRequest());
    }

    @Test
    void datosDeVigenciaNoDisponiblesBloqueanConCodigoEspecifico() throws Exception {
        conSesion(sesion(null, null, 0));
        String token = jwtService.generarToken(1, "ADM20240001", List.of("ADMIN"));
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletResponse response = ejecutar("/usuarios", token, chain);

        assertEquals(403, response.getStatus());
        String cuerpo = response.getContentAsString();
        assertTrue(cuerpo.contains("VIGENCIA_NO_DISPONIBLE"), cuerpo);
        assertTrue(cuerpo.contains("No se pudo verificar la vigencia de su cuenta."), cuerpo);
        assertNull(chain.getRequest());
    }

    @Test
    void rutaPermitidaConVigenciaVencidaNoConsultaElRepositorio() throws Exception {
        conSesion(sesion(FechaColegio.hoy().minusDays(30), FechaColegio.hoy().minusDays(1), 0));
        String token = jwtService.generarToken(1, "ADM20240001", List.of("ADMIN"));
        MockFilterChain chain = new MockFilterChain();
        org.mockito.Mockito.clearInvocations(usuarioRepositoryProvider, usuarioRepository);

        MockHttpServletResponse response = ejecutar("/auth/logout", token, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
        org.mockito.Mockito.verifyNoInteractions(usuarioRepositoryProvider, usuarioRepository);
    }

    @Test
    void vigenciaValidaDejaPasarLaPeticion() throws Exception {
        conSesion(sesion(FechaColegio.hoy().minusDays(1), FechaColegio.hoy(), 0));
        String token = jwtService.generarToken(1, "ADM20240001", List.of("ADMIN"));
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletResponse response = ejecutar("/usuarios", token, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }
}
