package com.example.Escolar.Service;

import com.example.Escolar.Dto.DiaFeriadoRequest;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.DiaFeriadoRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Cada feriado es de un año escolar y su fecha cae dentro de ese año. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DiaFeriadoServiceTest {
    @Mock DiaFeriadoRepository diaFeriadoRepository;
    @Mock AnioEscolarRepository anioEscolarRepository;
    @Mock AccesoRepository accesoRepository;
    @InjectMocks DiaFeriadoService service;

    private final Acceso activo = new Acceso(AccesoConstants.ACTIVO, "Activo");
    private final Acceso eliminado = new Acceso(AccesoConstants.ELIMINADO, "Eliminado");

    @BeforeEach
    void preparar() {
        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(activo));
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        AnioEscolar anio = new AnioEscolar();
        anio.setIdAnio(1);
        anio.setAnio("2026");
        anio.setFechaInicio(LocalDate.of(2026, 3, 2));
        anio.setFechaFin(LocalDate.of(2026, 12, 18));
        when(anioEscolarRepository.findByIdAnioAndAccesoNot(1, eliminado)).thenReturn(Optional.of(anio));
        when(diaFeriadoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private static DiaFeriadoRequest pedido(LocalDate fecha, Integer idAnio) {
        DiaFeriadoRequest r = new DiaFeriadoRequest();
        r.setFecha(fecha);
        r.setMotivo("Fiestas Patrias");
        r.setIdAnioEscolar(idAnio);
        return r;
    }

    @Test
    void sinAnioEscolarNoSeRegistra() {
        assertThrows(IllegalArgumentException.class, () -> service.create(pedido(LocalDate.of(2026, 7, 28), null)));
        verify(diaFeriadoRepository, never()).save(any());
    }

    @Test
    void laFechaDebeEstarDentroDelAnio() {
        var ex = assertThrows(IllegalArgumentException.class, () -> service.create(pedido(LocalDate.of(2027, 1, 1), 1)));
        assertTrue(ex.getMessage().contains("2026"), ex.getMessage());
        assertThrows(IllegalArgumentException.class, () -> service.create(pedido(LocalDate.of(2026, 3, 1), 1)));
        verify(diaFeriadoRepository, never()).save(any());
    }

    @Test
    void dentroDelAnioSeRegistra() {
        var creado = service.create(pedido(LocalDate.of(2026, 7, 28), 1));
        assertEquals(1, creado.getIdAnioEscolar());
        // Los extremos del año también valen.
        assertDoesNotThrow(() -> service.create(pedido(LocalDate.of(2026, 3, 2), 1)));
        assertDoesNotThrow(() -> service.create(pedido(LocalDate.of(2026, 12, 18), 1)));
    }
}
