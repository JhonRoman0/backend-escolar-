package com.example.Escolar.Service;

import com.example.Escolar.Dto.JustificacionRequest;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Justificacion;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.JustificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** La fecha del motivo es la de registro: la pone el sistema y no cambia al editar. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JustificacionServiceTest {
    @Mock JustificacionRepository justificacionRepository;
    @Mock AccesoRepository accesoRepository;
    @InjectMocks JustificacionService service;

    private final Acceso activo = new Acceso(AccesoConstants.ACTIVO, "Activo");
    private final Acceso eliminado = new Acceso(AccesoConstants.ELIMINADO, "Eliminado");

    @BeforeEach
    void preparar() {
        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(activo));
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        when(justificacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private static JustificacionRequest pedido(String motivo) {
        JustificacionRequest r = new JustificacionRequest();
        r.setMotivo(motivo);
        return r;
    }

    @Test
    void alCrearLaFechaDeRegistroEsHoy() {
        var creado = service.create(pedido("  Cita médica "));
        assertEquals(LocalDate.now(), creado.getFechaRegistro());
        assertEquals("Cita médica", creado.getMotivo());
    }

    @Test
    void alEditarLaFechaDeRegistroNoCambia() {
        Justificacion existente = new Justificacion();
        existente.setIdJustificacion(4);
        existente.setMotivo("Cita");
        existente.setFechaRegistro(LocalDate.of(2026, 3, 2));
        existente.setAcceso(activo);
        when(justificacionRepository.findByIdJustificacionAndAccesoNot(4, eliminado)).thenReturn(Optional.of(existente));

        var editado = service.update(4, pedido("Cita médica"));

        assertEquals(LocalDate.of(2026, 3, 2), editado.getFechaRegistro());
        assertEquals("Cita médica", editado.getMotivo());
    }
}
