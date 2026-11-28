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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthFilterVigenciaTest {

    private final JwtService jwtService = new JwtService("clave-secreta-de-pruebas-con-32-bytes-o-mas!!", 3_600_000);
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ObjectProvider<UsuarioRepository> usuarioRepositoryProvider;
    @Mock
    private ConcesionesService concesionesService;
    private JwtAuthFilter filter;

    @BeforeEach
    void prepararFiltro() {
        when(usuarioRepositoryProvider.getObject()).thenReturn(usuarioRepository);
        lenient().when(concesionesService.rolActivoValido(eq(1), anyInt())).thenReturn(true);
        filter = new JwtAuthFilter(jwtService, usuarioRepositoryProvider, new VigenciaService(), concesionesService);
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
    void vigenciaVencidaBloqueaConsultaDeRoles() throws Exception {
        LocalDate inicio = FechaColegio.hoy().minusDays(30);
        LocalDate fin = FechaColegio.hoy().minusDays(1);
        conSesion(sesion(inicio, fin, 0));
        String token = jwtService.generarToken(1, "ADM20240001", List.of("ADMIN"));
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletResponse response = ejecutar("/auth/roles", token, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("VIGENCIA_VENCIDA"));
        assertNull(chain.getRequest());
    }

    @Test
    void vigenciaVencidaBloqueaCambioDeRol() throws Exception {
        LocalDate inicio = FechaColegio.hoy().minusDays(30);
        LocalDate fin = FechaColegio.hoy().minusDays(1);
        conSesion(sesion(inicio, fin, 0));
        String token = jwtService.generarToken(1, "ADM20240001", List.of("ADMIN"));
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletResponse response = ejecutar("/auth/cambiar-rol", token, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("VIGENCIA_VENCIDA"));
        assertNull(chain.getRequest());
    }

    @Test
    void principalAutenticadoExponeVersionTokenDelJwt() throws Exception {
        conSesion(sesion(FechaColegio.hoy().minusDays(1), null, 4));
        when(usuarioRepository.tieneDispositivo(1, 40)).thenReturn(true);
        String token = jwtService.generarToken(1, "ADM20240001", 7, List.of("ADMIN"), false, null, 4, 40);
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletResponse response = ejecutar("/auth/me", token, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
        UsuarioAutenticado principal = (UsuarioAutenticado) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        assertEquals(4, principal.versionToken());
        assertEquals(7, principal.idRolActivo());
        assertEquals(40, principal.idDispositivo());
    }

    @Test
    void sesionDeUnDispositivoQuitadoQuedaSinAutenticar() throws Exception {
        conSesion(sesion(FechaColegio.hoy().minusDays(1), null, 0));
        when(usuarioRepository.tieneDispositivo(1, 40)).thenReturn(false);
        when(usuarioRepository.tieneDispositivo(1, 41)).thenReturn(true);
        String quitado = jwtService.generarToken(1, "ADM20240001", 7, List.of("ADMIN"), false, null, 0, 40);
        String otro = jwtService.generarToken(1, "ADM20240001", 7, List.of("ADMIN"), false, null, 0, 41);

        // Sin autenticación: Spring responde 401 y el navegador vuelve al login.
        ejecutar("/auth/me", quitado, new MockFilterChain());
        assertNull(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());

        // La sesión de otro dispositivo del mismo usuario sigue valiendo.
        MockFilterChain chain = new MockFilterChain();
        ejecutar("/usuarios", otro, chain);
        assertNotNull(chain.getRequest());
        var principal = (com.example.Escolar.Security.UsuarioAutenticado)
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertEquals(41, principal.idDispositivo());
        assertEquals(7, principal.idRolActivo());
    }

    @Test
    void sesionSinDispositivoNoConsultaDispositivos() throws Exception {
        conSesion(sesion(FechaColegio.hoy().minusDays(1), null, 0));
        String token = jwtService.generarToken(1, "ADM20240001", 7, List.of("ADMIN"), false, null, 0, null);
        MockFilterChain chain = new MockFilterChain();

        ejecutar("/usuarios", token, chain);

        assertNotNull(chain.getRequest());
        org.mockito.Mockito.verify(usuarioRepository, org.mockito.Mockito.never())
                .tieneDispositivo(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
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
        clearInvocations(usuarioRepositoryProvider, usuarioRepository, concesionesService);

        MockHttpServletResponse response = ejecutar("/auth/logout", token, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
        verifyNoInteractions(usuarioRepositoryProvider, usuarioRepository, concesionesService);
    }

    @Test
    void cuentaInactivaBloqueaConCodigoEspecificoYUnaSolaConsulta() throws Exception {
        UsuarioVigenciaSesion sesionInactiva = new UsuarioVigenciaSesion() {
            @Override
            public Integer getIdUsuario() {
                return 1;
            }

            @Override
            public LocalDate getFechaInicioVigencia() {
                return FechaColegio.hoy().minusDays(1);
            }

            @Override
            public LocalDate getFechaFinVigencia() {
                return null;
            }

            @Override
            public Integer getVersionToken() {
                return 0;
            }

            @Override
            public Long getAccesoId() {
                return AccesoConstants.INACTIVO;
            }
        };
        conSesion(sesionInactiva);
        String token = jwtService.generarToken(1, "ADM20240001", List.of("ADMIN"));
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletResponse response = ejecutar("/usuarios", token, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("VIGENCIA_CUENTA_INACTIVA"),
                response.getContentAsString());
        assertNull(chain.getRequest());
        org.mockito.Mockito.verify(usuarioRepository, org.mockito.Mockito.times(1))
                .findVigenciaSesion(eq(1), eq(AccesoConstants.ELIMINADO));
    }

    @Test
    void cuentaInactivaNoBloqueaLogoutSinConsultarRepositorio() throws Exception {
        conSesion(sesion(FechaColegio.hoy().minusDays(1), null, 0));
        UsuarioVigenciaSesion sesionInactiva = org.mockito.Mockito.mock(UsuarioVigenciaSesion.class);
        org.mockito.Mockito.when(sesionInactiva.getAccesoId()).thenReturn(AccesoConstants.INACTIVO);
        conSesion(sesionInactiva);
        String token = jwtService.generarToken(1, "ADM20240001", List.of("ADMIN"), false, null, 0, 40);
        MockFilterChain chain = new MockFilterChain();
        clearInvocations(usuarioRepositoryProvider, usuarioRepository, concesionesService);

        MockHttpServletResponse response = ejecutar("/auth/logout", token, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
        verifyNoInteractions(usuarioRepositoryProvider, usuarioRepository, concesionesService);
    }

    @Test
    void vigenciaValidaDejaPasarLaPeticion() throws Exception {
        conSesion(sesion(FechaColegio.hoy().minusDays(1), FechaColegio.hoy(), 0));
        String token = jwtService.generarToken(1, "ADM20240001", 7, List.of("ADMIN"), false, null, 0, null);
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletResponse response = ejecutar("/usuarios", token, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    @Test
    void sinCookieEnIdentidadNoAutenticaYPasaLaCadena() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", SecurityConfig.RUTA_IDENTIDAD_COLEGIO);
        request.setServletPath(SecurityConfig.RUTA_IDENTIDAD_COLEGIO);
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        clearInvocations(usuarioRepositoryProvider, usuarioRepository, concesionesService);

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(usuarioRepositoryProvider, usuarioRepository, concesionesService);
    }

    @Test
    void cookieVencidaEnIdentidadNoAutenticaYPasaLaCadena() throws Exception {
        String token = jwtService.generarToken(1, "ADM20240001", List.of("ADMIN"), false,
                FechaColegio.hoy().minusDays(1), 0);
        MockFilterChain chain = new MockFilterChain();
        clearInvocations(usuarioRepositoryProvider, usuarioRepository, concesionesService);

        ejecutar(SecurityConfig.RUTA_IDENTIDAD_COLEGIO, token, chain);

        assertNotNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(usuarioRepositoryProvider, usuarioRepository, concesionesService);
    }

    @Test
    void cookieObsoletaBloqueaIdentidadCon403() throws Exception {
        conSesion(sesion(FechaColegio.hoy().minusDays(1), null, 2));
        String token = jwtService.generarToken(1, "ADM20240001", List.of("ADMIN"), false, null, 1);
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletResponse response = ejecutar(SecurityConfig.RUTA_IDENTIDAD_COLEGIO, token, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("VIGENCIA_TOKEN_OBSOLETO"));
        assertNull(chain.getRequest());
    }

    @Test
    void tokenSinIdRolActivoBloqueaRutaCon403() throws Exception {
        conSesion(sesion(FechaColegio.hoy().minusDays(1), null, 0));
        String token = jwtService.generarToken(1, "ADM20240001", List.of("ADMIN"));
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletResponse response = ejecutar("/usuarios", token, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("VIGENCIA_ROL_NO_DISPONIBLE"));
        assertNull(chain.getRequest());
        verify(concesionesService, never()).rolActivoValido(any(), any());
    }

    @Test
    void rolActivoNoDisponibleBloqueaRutaConUnaSolaDelegacionAlServicio() throws Exception {
        conSesion(sesion(FechaColegio.hoy().minusDays(1), null, 0));
        when(concesionesService.rolActivoValido(eq(1), eq(7))).thenReturn(false);
        String token = jwtService.generarToken(1, "ADM20240001", 7, List.of("ADMIN"), false, null, 0, null);
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletResponse response = ejecutar("/usuarios", token, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("VIGENCIA_ROL_NO_DISPONIBLE"));
        assertNull(chain.getRequest());
        verify(concesionesService, times(1)).rolActivoValido(1, 7);
        verify(usuarioRepository, times(1)).findVigenciaSesion(eq(1), eq(AccesoConstants.ELIMINADO));
    }

    @Test
    void rolActivoNoDisponibleConservaMeYLogoutSinConsultarConcesiones() throws Exception {
        conSesion(sesion(FechaColegio.hoy().minusDays(1), null, 0));
        when(concesionesService.rolActivoValido(eq(1), eq(7))).thenReturn(false);
        String token = jwtService.generarToken(1, "ADM20240001", 7, List.of("ADMIN"), false, null, 0, null);
        clearInvocations(concesionesService);

        for (String ruta : List.of("/auth/me", "/auth/logout")) {
            MockFilterChain chain = new MockFilterChain();

            MockHttpServletResponse response = ejecutar(ruta, token, chain);

            assertEquals(200, response.getStatus(), ruta);
            assertNotNull(chain.getRequest(), ruta);
        }
        verifyNoInteractions(concesionesService);
    }
}
