package com.example.Escolar.Service;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Grado;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.GradoRepository;
import com.example.Escolar.Repository.NivelRepository;
import com.example.Escolar.Service.OrdenAcademicoService.Posicion;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrdenAcademicoServiceTest {

    @Mock
    private NivelRepository nivelRepository;
    @Mock
    private GradoRepository gradoRepository;
    @Mock
    private AccesoRepository accesoRepository;
    @InjectMocks
    private OrdenAcademicoService orden;

    private final Nivel primaria = nivel(2, 2);
    private final Nivel secundaria = nivel(3, 3);
    private final Grado quinto = grado(8, primaria, 5);
    private final Grado sexto = grado(9, primaria, 6);
    private final Grado primeroSec = grado(10, secundaria, 1);

    @BeforeEach
    void setUp() {
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(new Acceso()));
        when(nivelRepository.findByAccesoNot(any())).thenReturn(List.of(primaria, secundaria));
        when(gradoRepository.findByNivelAndAccesoNot(eq(primaria), any())).thenReturn(List.of(quinto, sexto));
        when(gradoRepository.findByNivelAndAccesoNot(eq(secundaria), any())).thenReturn(List.of(primeroSec));
    }

    private static Nivel nivel(int id, int orden) {
        Nivel nivel = new Nivel();
        nivel.setIdNivel(id);
        nivel.setOrden(orden);
        return nivel;
    }

    private static Grado grado(int id, Nivel nivel, int orden) {
        Grado grado = new Grado();
        grado.setIdGrado(id);
        grado.setNivel(nivel);
        grado.setOrden(orden);
        return grado;
    }

    @Test
    void elSiguienteEsElDeOrdenInmediatoEnElNivel() {
        assertEquals(Optional.of(new Posicion(2, 6)), orden.siguiente(quinto));
    }

    @Test
    void despuesDelUltimoGradoVieneElPrimeroDelNivelSiguiente() {
        assertEquals(Optional.of(new Posicion(3, 1)), orden.siguiente(sexto));
    }

    @Test
    void subirIntercambiaElOrdenConElGradoAnterior() {
        when(gradoRepository.findByIdGradoAndAccesoNot(eq(9), any())).thenReturn(Optional.of(sexto));

        orden.moverGrado(9, true);

        assertEquals(5, sexto.getOrden());
        assertEquals(6, quinto.getOrden());
    }

    @Test
    void elPrimeroDelNivelNoPuedeSubir() {
        when(gradoRepository.findByIdGradoAndAccesoNot(eq(10), any())).thenReturn(Optional.of(primeroSec));

        assertThrows(IllegalArgumentException.class, () -> orden.moverGrado(10, true));
    }
}
