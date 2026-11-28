package com.example.Escolar.Controller;

import com.example.Escolar.Dto.LoginResponse;
import com.example.Escolar.Exception.GlobalExceptionHandler;
import com.example.Escolar.Security.JwtService;
import com.example.Escolar.Security.UsuarioAutenticado;
import com.example.Escolar.Service.AuthService;
import com.example.Escolar.Service.DosFactorService;
import com.example.Escolar.Service.RolAsignadoResolver;
import com.example.Escolar.Service.SeleccionRolDesafioService;
import com.example.Escolar.Service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerCambiarRolTest {

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
    @Mock
    private RolAsignadoResolver rolAsignadoResolver;

    @InjectMocks
    private AuthController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(controller, "jwtExpirationMs", 3_600_000L);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        UsuarioAutenticado principal = new UsuarioAutenticado(7, "ADM20240001",
                List.of("Docente"), 40, 1, 3);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cambiarRolUsaSoloElPrincipalAutenticadoYReenviaCookie() throws Exception {
        LoginResponse response = new LoginResponse();
        response.setToken("jwt-rol-2");
        when(authService.cambiarRol(7, 2, 1, 40, 3)).thenReturn(response);

        mockMvc.perform(post("/auth/cambiar-rol")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idRol\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-rol-2"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("escolar_token=jwt-rol-2")));

        verify(authService).cambiarRol(7, 2, 1, 40, 3);
    }

    @Test
    void cambiarRolRechazaSolicitudSinIdRol() throws Exception {
        mockMvc.perform(post("/auth/cambiar-rol")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(authService, never()).cambiarRol(any(), any(), any(), any(), any());
    }
}
