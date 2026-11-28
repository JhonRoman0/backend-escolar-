package com.example.Escolar.Controller;

import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.HorarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HorarioControllerTest {
    @Mock HorarioService horarioService;
    @Mock AccesoContextoService accesoContextoService;
    @InjectMocks HorarioController controller;

    private final Pageable pageable = Pageable.ofSize(20);
    private final Integer idRolActivo = 1;

    @BeforeEach
    void preparar() {
        when(accesoContextoService.idUsuarioAutenticado()).thenReturn(7);
        when(accesoContextoService.idRolActivoAutenticado()).thenReturn(idRolActivo);
    }

    @Test
    void docenteCanonicoGanaAliasYAnioEscolarGanaIdAnio() {
        controller.listar(null, null, 5, 9, 2, null, null, null, null, null, 3, pageable);

        ArgumentCaptor<Integer> docente = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> anio = ArgumentCaptor.forClass(Integer.class);
        verify(horarioService).listar(any(), any(), docente.capture(), anio.capture(),
                any(), any(), any(), any(), any(), eq(7), eq(idRolActivo), eq(pageable));
        assertEquals(5, docente.getValue());
        assertEquals(2, anio.getValue());
    }

    @Test
    void aliasIdDocenteSeUsaCuandoNoLlegaDocente() {
        controller.listar(null, null, null, 9, null, null, null, null, null, null, 2, pageable);

        ArgumentCaptor<Integer> docente = ArgumentCaptor.forClass(Integer.class);
        verify(horarioService).listar(any(), any(), docente.capture(), eq(2),
                any(), any(), any(), any(), any(), eq(7), eq(idRolActivo), eq(pageable));
        assertEquals(9, docente.getValue());
    }
}
