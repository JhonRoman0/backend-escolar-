package com.example.Escolar.Security;

import com.example.Escolar.Controller.AuthController;
import com.example.Escolar.Service.AuthService;
import com.example.Escolar.Service.DosFactorService;
import com.example.Escolar.Service.EventosService;
import com.example.Escolar.Service.RolAsignadoResolver;
import com.example.Escolar.Service.SeleccionRolDesafioService;
import com.example.Escolar.Service.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.expiration=3600000")
class AuthRutasSeguridadTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private UsuarioService usuarioService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private DosFactorService dosFactorService;
    @MockitoBean
    private SeleccionRolDesafioService seleccionRolDesafioService;
    @MockitoBean
    private RolAsignadoResolver rolAsignadoResolver;
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;
    @MockitoBean
    private RbacAuthorizationManager rbacAuthorizationManager;
    @MockitoBean
    private EventosService eventosService;

    @BeforeEach
    void preparar() throws Exception {
        doAnswer(invocation -> {
            FilterChain cadena = invocation.getArgument(2);
            cadena.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
        when(rbacAuthorizationManager.authorize(any(), any())).thenReturn(new AuthorizationDecision(false));
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rolesRechazaSolicitudNoAutenticada() throws Exception {
        mockMvc.perform(get("/auth/roles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verifyNoInteractions(rolAsignadoResolver);
    }

    @Test
    void cambiarRolRechazaSolicitudNoAutenticada() throws Exception {
        mockMvc.perform(post("/auth/cambiar-rol")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idRol\":2}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verifyNoInteractions(authService);
    }
}
