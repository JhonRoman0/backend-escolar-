package com.example.Escolar.Controller;

import com.example.Escolar.Dto.LoginResponse;
import com.example.Escolar.Dto.RolAsignadoSesionResponse;
import com.example.Escolar.Exception.GlobalExceptionHandler;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Security.JwtService;
import com.example.Escolar.Service.AuthService;
import com.example.Escolar.Service.DosFactorService;
import com.example.Escolar.Service.SeleccionRolDesafioService;
import com.example.Escolar.Service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ExtendWith(MockitoExtension.class)
class AuthControllerSeleccionRolTest {

    @Mock
    private AuthService authService;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private JwtService jwtService;
    @Mock
    private DosFactorService dosFactorService;
    @Mock
    private SeleccionRolDesafioService seleccionRolDesafioService;

    @InjectMocks
    private AuthController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(controller, "jwtExpirationMs", 3_600_000L);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void loginConSeleccionDeRolNoEmiteCookieDeSesion() throws Exception {
        Usuario usuario = new Usuario();
        LoginResponse respuesta = new LoginResponse();
        respuesta.setRequiereSeleccionRol(true);
        respuesta.setDesafioSeleccionRol("SELROL:0:40:abc");
        when(authService.validarCredenciales(any())).thenReturn(usuario);
        when(authService.iniciarSesion(usuario, null)).thenReturn(respuesta);

        MvcResult resultado = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "codigo", "ADM20240001", "contraseña", "clave"))))
                .andReturn();

        assertEquals(200, resultado.getResponse().getStatus());
        assertFalse(tieneCookie(resultado, "escolar_token="));
    }

    @Test
    void verificarDosFactorConSeleccionConservaCookieDeDispositivoPeroNoSesion() throws Exception {
        Usuario usuario = new Usuario();
        LoginResponse respuesta = new LoginResponse();
        respuesta.setRequiereSeleccionRol(true);
        respuesta.setDesafioSeleccionRol("SELROL:0:40:abc");
        when(dosFactorService.verificar(any(), any(), anyBoolean(), any(), any()))
                .thenReturn(new DosFactorService.Verificado(usuario, "token-dispositivo", 40));
        when(authService.iniciarSesion(usuario, 40)).thenReturn(respuesta);

        MvcResult resultado = mockMvc.perform(post("/auth/2fa/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "desafio", "2FA:abc", "codigo", "123456"))))
                .andReturn();

        assertEquals(200, resultado.getResponse().getStatus());
        assertTrue(tieneCookie(resultado, DosFactorService.COOKIE_DISPOSITIVO + "="));
        assertFalse(tieneCookie(resultado, "escolar_token="));
    }

    @Test
    void reenviarDosFactorNoEmiteCookieDeSesion() throws Exception {
        when(dosFactorService.reenviar(eq("2FA:abc"), any())).thenReturn(
                new DosFactorService.Desafio("2FA:abc", "an***@gmail.com", 45, 5));

        MvcResult resultado = mockMvc.perform(post("/auth/2fa/reenviar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("desafio", "2FA:abc"))))
                .andReturn();

        assertEquals(200, resultado.getResponse().getStatus());
        assertFalse(tieneCookie(resultado, "escolar_token="));
        verify(authService, never()).iniciarSesion(any(), any());
    }

    @Test
    void seleccionDeRolEmiteCookieDeSesionTrasConsumirElDesafio() throws Exception {
        Usuario usuario = new Usuario();
        RolAsignadoSesionResponse rol = new RolAsignadoSesionResponse(2, "Docente",
                RolAsignadoSesionResponse.EstadoRol.ACTIVO);
        SeleccionRolDesafioService.Consumido consumido =
                new SeleccionRolDesafioService.Consumido(usuario, rol, 40);
        LoginResponse respuesta = new LoginResponse();
        respuesta.setToken("jwt");
        when(seleccionRolDesafioService.consumir("SELROL:0:40:abc", 2)).thenReturn(consumido);
        when(authService.emitirSesion(usuario, 40, rol)).thenReturn(respuesta);

        MvcResult resultado = mockMvc.perform(post("/auth/seleccion-rol")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "desafio", "SELROL:0:40:abc", "idRol", 2))))
                .andReturn();

        assertEquals(200, resultado.getResponse().getStatus());
        assertTrue(tieneCookie(resultado, "escolar_token=jwt"));
        verify(authService).emitirSesion(eq(usuario), eq(40), eq(rol));
    }

    private boolean tieneCookie(MvcResult resultado, String prefijo) {
        return resultado.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
                .anyMatch(valor -> valor.contains(prefijo));
    }
}
