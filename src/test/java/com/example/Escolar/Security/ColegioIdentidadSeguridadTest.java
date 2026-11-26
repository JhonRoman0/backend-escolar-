package com.example.Escolar.Security;

import com.example.Escolar.Controller.ColegioController;
import com.example.Escolar.Dto.IdentidadInstitucionalResponse;
import com.example.Escolar.Service.ColegioService;
import com.example.Escolar.Service.EventosService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ColegioController.class)
@Import(SecurityConfig.class)
class ColegioIdentidadSeguridadTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ColegioService colegioService;

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

        IdentidadInstitucionalResponse identidad = new IdentidadInstitucionalResponse();
        identidad.setNombre("IE Prueba");
        identidad.setLogoUrl("https://cdn/logo");
        when(colegioService.getIdentidadPublica()).thenReturn(identidad);
    }

    @Test
    void identidadEsAccesibleSinAutenticacion() throws Exception {
        mockMvc.perform(get(SecurityConfig.RUTA_IDENTIDAD_COLEGIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("IE Prueba"))
                .andExpect(jsonPath("$.logoUrl").value("https://cdn/logo"));
    }

    @Test
    void actualSigueProtegidoSinAutenticacion() throws Exception {
        mockMvc.perform(get("/colegios/actual"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listadoSigueProtegidoSinAutenticacion() throws Exception {
        mockMvc.perform(get("/colegios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void soloElGetDeIdentidadEsPublico() throws Exception {
        mockMvc.perform(post(SecurityConfig.RUTA_IDENTIDAD_COLEGIO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }
}
