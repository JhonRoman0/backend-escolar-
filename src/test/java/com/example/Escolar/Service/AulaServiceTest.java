package com.example.Escolar.Service;

import com.example.Escolar.Dto.AulaRequest;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Aula;
import com.example.Escolar.Model.HorarioClase;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AulaRepository;
import com.example.Escolar.Repository.HorarioClaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AulaServiceTest {

    @Mock
    private AulaRepository aulaRepository;
    @Mock
    private AccesoRepository accesoRepository;
    @Mock
    private HorarioClaseRepository horarioClaseRepository;
    @InjectMocks
    private AulaService aulaService;

    private Acceso activo;
    private Acceso eliminado;

    @BeforeEach
    void setUp() {
        activo = new Acceso();
        activo.setIdAcceso(AccesoConstants.ACTIVO);
        eliminado = new Acceso();
        eliminado.setIdAcceso(AccesoConstants.ELIMINADO);

        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(activo));
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        when(aulaRepository.findByAccesoNot(eliminado)).thenReturn(new ArrayList<>());
        when(aulaRepository.save(any(Aula.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aulaRepository.findByNombreIgnoreCase(any())).thenReturn(Optional.empty());
        when(aulaRepository.findByNombreIgnoreCaseAndIdAulaNot(any(), any())).thenReturn(Optional.empty());
        when(aulaRepository.findByIdAulaAndAccesoNot(any(), any())).thenAnswer(inv ->
                Optional.of(aula(inv.getArgument(0), "Aula 101", 30)));
    }

    private Aula aula(Integer id, String nombre, Integer capacidad) {
        Aula a = new Aula();
        a.setIdAula(id);
        a.setNombre(nombre);
        a.setCapacidad(capacidad);
        a.setAcceso(activo);
        return a;
    }

    private AulaRequest request(String nombre, Integer capacidad, Long accesoId) {
        AulaRequest r = new AulaRequest();
        r.setNombre(nombre);
        r.setCapacidad(capacidad);
        r.setAccesoId(accesoId);
        return r;
    }

    @Test
    void crearAceptaNombreNormalizado() {
        AulaRequest r = request("  Aula 101  ", 25, AccesoConstants.ACTIVO);
        assertDoesNotThrow(() -> aulaService.create(r));

        ArgumentCaptor<Aula> captor = ArgumentCaptor.forClass(Aula.class);
        verify(aulaRepository).save(captor.capture());
        assertEquals("Aula 101", captor.getValue().getNombre());
        assertEquals(25, captor.getValue().getCapacidad());
    }

    @Test
    void crearRechazaNombreDuplicadoEnMayusculas() {
        when(aulaRepository.findByNombreIgnoreCase("AULA 101"))
                .thenReturn(Optional.of(aula(1, "Aula 101", 30)));
        AulaRequest r = request("AULA 101", 25, AccesoConstants.ACTIVO);
        assertThrows(IllegalArgumentException.class, () -> aulaService.create(r));
    }

    @Test
    void crearRechazaAccesoEliminado() {
        AulaRequest r = request("Aula 303", 30, AccesoConstants.ELIMINADO);
        assertThrows(IllegalArgumentException.class, () -> aulaService.create(r));
        verify(aulaRepository, never()).save(any(Aula.class));
    }

    @Test
    void actualizarRechazaNombreDuplicadoExcluyendoMismaAula() {
        when(aulaRepository.findByNombreIgnoreCaseAndIdAulaNot("Aula 202", 1))
                .thenReturn(Optional.of(aula(2, "Aula 202", 30)));
        AulaRequest r = request("Aula 202", 30, AccesoConstants.ACTIVO);
        assertThrows(IllegalArgumentException.class, () -> aulaService.update(1, r));
    }

    @Test
    void actualizarAceptaConservarMismoNombre() {
        // Discrimina el camino correcto: si el service validara sin excluir el id
        // usaría findByNombreIgnoreCase y este stub lo haría fallar.
        when(aulaRepository.findByNombreIgnoreCase("Aula 101"))
                .thenReturn(Optional.of(aula(1, "Aula 101", 30)));
        AulaRequest r = request("Aula 101", 40, AccesoConstants.ACTIVO);
        assertDoesNotThrow(() -> aulaService.update(1, r));
    }

    @Test
    void eliminarRechazaSiTieneHorariosActivos() {
        when(horarioClaseRepository.countByAulaIdAulaAndAccesoNot(1, eliminado)).thenReturn(2L);

        assertThrows(IllegalArgumentException.class, () -> aulaService.delete(1));
        verify(horarioClaseRepository, never()).deleteAll(anyList());
        verify(aulaRepository, never()).delete(any(Aula.class));
    }

    @Test
    void eliminarBorraFisicamenteYPurgaHorariosEnPapelera() {
        when(horarioClaseRepository.countByAulaIdAulaAndAccesoNot(1, eliminado)).thenReturn(0L);
        when(horarioClaseRepository.findByAulaIdAula(1))
                .thenReturn(new ArrayList<>(List.of(new HorarioClase())));

        aulaService.delete(1);

        verify(horarioClaseRepository).deleteAll(anyList());
        verify(aulaRepository).delete(any(Aula.class));
        verify(aulaRepository, never()).save(any(Aula.class));
    }
}
