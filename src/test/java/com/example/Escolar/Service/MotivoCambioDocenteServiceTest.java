package com.example.Escolar.Service;

import com.example.Escolar.Dto.MotivoCambioDocenteRequest;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.TipoCambioDocente;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.MotivoCambioDocenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MotivoCambioDocenteServiceTest {

    @Mock
    private MotivoCambioDocenteRepository motivoRepository;
    @Mock
    private AccesoRepository accesoRepository;
    @InjectMocks
    private MotivoCambioDocenteService service;

    private Acceso activo;
    private Acceso eliminado;

    @BeforeEach
    void setUp() {
        activo = new Acceso();
        activo.setIdAcceso((long) AccesoConstants.ACTIVO);
        eliminado = new Acceso();
        eliminado.setIdAcceso((long) AccesoConstants.ELIMINADO);
        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(activo));
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
    }

    @Test
    void sinMotivosSembradosDevuelveListasVacias() {
        when(motivoRepository.findByAccesoNot(eliminado)).thenReturn(List.of());

        assertTrue(service.getAll().isEmpty());
        assertTrue(service.getByTipo(TipoCambioDocente.TEMPORAL).isEmpty());
        assertTrue(service.getByTipo(TipoCambioDocente.DEFINITIVO).isEmpty());
    }

    @Test
    void noRepiteNombreIgnorandoMayusculas() {
        when(motivoRepository.existsByNombreIgnoreCaseAndAccesoNot("licencia", eliminado)).thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.create(new MotivoCambioDocenteRequest("  licencia  ", true, true)));

        assertTrue(error.getMessage().contains("Ya existe"));
    }

    @Test
    void creaMotivoConFechaDeLaZonaDelColegio() {
        when(motivoRepository.existsByNombreIgnoreCaseAndAccesoNot("Licencia", eliminado)).thenReturn(false);
        when(motivoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var respuesta = service.create(new MotivoCambioDocenteRequest(" Licencia ", true, false));

        assertEquals("Licencia", respuesta.nombre());
        assertEquals(true, respuesta.aplicaTemporal());
        assertEquals(false, respuesta.aplicaDefinitivo());
    }
}
