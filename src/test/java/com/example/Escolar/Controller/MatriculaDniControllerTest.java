package com.example.Escolar.Controller;

import com.example.Escolar.Exception.GlobalExceptionHandler;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.ExclusividadDniService;
import com.example.Escolar.Service.InscripcionService;
import com.example.Escolar.Service.MatriculaReglasService;
import com.example.Escolar.Service.MatriculaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MatriculaDniControllerTest {
    @Mock private ExclusividadDniService exclusividadDni;
    @Mock private MatriculaService matriculas;
    @Mock private InscripcionService inscripciones;
    @Mock private MatriculaReglasService reglas;
    @Mock private AccesoContextoService acceso;
    @InjectMocks private MatriculaController controller;
    private MockMvc mvc;

    @BeforeEach
    void prepararMvc() {
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void consultaCompatibleRespondeSinContenidoNiFichasPersonales() throws Exception {
        mvc.perform(get("/matriculas/inscripcion/alumno/validar-dni/12345678"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(exclusividadDni).validarAlumnoInscripcion("12345678");
        verifyNoInteractions(inscripciones, matriculas, reglas, acceso);
    }

    @Test
    void conflictoConservaElFormatoActualDeError() throws Exception {
        String mensaje = "Este DNI ya está registrado como usuario y no puede registrarse como alumno";
        doThrow(new IllegalArgumentException(mensaje)).when(exclusividadDni).validarAlumnoInscripcion("12345678");
        var resultado = mvc.perform(get("/matriculas/inscripcion/alumno/validar-dni/12345678"))
                .andExpect(status().isBadRequest()).andReturn();
        assertTrue(resultado.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8).contains(mensaje));
        verifyNoInteractions(inscripciones, matriculas, reglas, acceso);
    }
}
