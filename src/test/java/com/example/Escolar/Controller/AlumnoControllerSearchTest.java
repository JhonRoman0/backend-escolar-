package com.example.Escolar.Controller;

import com.example.Escolar.Dto.AlumnoResponse;
import com.example.Escolar.Exception.GlobalExceptionHandler;
import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.AlumnoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AlumnoControllerSearchTest {
    @Mock private AlumnoService alumnoService;
    @Mock private AccesoContextoService accesoContextoService;
    @InjectMocks private AlumnoController controller;
    private MockMvc mvc;

    private final List<String> roles = List.of("ADMIN");

    @BeforeEach
    void prepararMvc() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AlumnoResponse alumno(int id, String nombre) {
        AlumnoResponse alumno = new AlumnoResponse();
        alumno.setIdAlumno(id);
        alumno.setNombre(nombre);
        return alumno;
    }

    @Test
    void propagaSearchYFiltrosYSerializaElTotal() throws Exception {
        List<AlumnoResponse> alumnos = List.of(alumno(7, "Ana"), alumno(8, "Ana María"));
        when(accesoContextoService.idUsuarioAutenticado()).thenReturn(50);
        when(accesoContextoService.rolesAutenticados()).thenReturn(roles);
        when(alumnoService.getAll(any(Pageable.class), eq(50), eq(roles),
                isNull(), isNull(), isNull(), isNull(), isNull(), eq(6), eq("ana")))
                .thenReturn(new PageImpl<>(alumnos, PageRequest.of(0, 10), 2));

        mvc.perform(get("/alumnos")
                        .param("page", "0")
                        .param("size", "10")
                        .param("idAnio", "6")
                        .param("search", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].idAlumno").value(7));

        verify(alumnoService).getAll(any(Pageable.class), eq(50), eq(roles),
                isNull(), isNull(), isNull(), isNull(), isNull(), eq(6), eq("ana"));
    }
}
