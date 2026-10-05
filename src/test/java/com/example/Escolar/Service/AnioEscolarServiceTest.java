package com.example.Escolar.Service;

import com.example.Escolar.Dto.AnioEscolarRequest;
import com.example.Escolar.Dto.AnioEscolarResponse;
import com.example.Escolar.Exception.AnioEscolarConGradosException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnioEscolarServiceTest {

    @Mock
    private AnioEscolarRepository anioEscolarRepository;
    @Mock
    private AccesoRepository accesoRepository;
    @Mock
    private GradoSeccionRepository gradoSeccionRepository;
    @Mock
    private AsignacionRepository asignacionRepository;
    @InjectMocks
    private AnioEscolarService anioEscolarService;

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
        when(anioEscolarRepository.findByAnioAndAccesoNot(anyString(), any(Acceso.class)))
                .thenReturn(Optional.empty());
        when(anioEscolarRepository.save(any(AnioEscolar.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(anioEscolarRepository.findByAccesoNot(any(Acceso.class)))
                .thenReturn(List.of());
        when(anioEscolarRepository.findByEstadoInAndFechaFinBeforeAndAccesoNot(
                anyList(), any(LocalDate.class), any(Acceso.class)))
                .thenReturn(List.of());
    }

    private AnioEscolar anio(Integer id, String anio, byte estado, LocalDate fin) {
        AnioEscolar a = new AnioEscolar();
        a.setIdAnio(id);
        a.setAnio(anio);
        a.setEstado(estado);
        a.setFechaFin(fin);
        a.setAcceso(activo);
        return a;
    }

    private AnioEscolarRequest request(String anio, Byte estado, LocalDate fin) {
        AnioEscolarRequest r = new AnioEscolarRequest();
        r.setAnio(anio);
        r.setEstado(estado);
        r.setFechaFin(fin);
        return r;
    }

    // ── Ventana de dos años ───────────────────────────────────────────────
    //
    // Los años se derivan del reloj para que los tests no envejezcan: si
    // escribieran "2026" fijo, en enero de 2027 empezarían a fallar solos.

    @Test
    void crearElAnioActualAcepta() {
        int actual = LocalDate.now().getYear();
        AnioEscolarRequest r = request(String.valueOf(actual), null, LocalDate.of(actual, 12, 15));

        AnioEscolarResponse resultado = anioEscolarService.create(r);

        assertEquals(actual, Integer.parseInt(resultado.getAnio()));
    }

    @Test
    void crearElAnioSiguienteAcepta() {
        int siguiente = LocalDate.now().getYear() + 1;
        AnioEscolarRequest r = request(String.valueOf(siguiente), null, LocalDate.of(siguiente, 12, 15));

        AnioEscolarResponse resultado = anioEscolarService.create(r);

        assertEquals(siguiente, Integer.parseInt(resultado.getAnio()));
    }

    @Test
    void crearMasAllaDelAnioSiguienteFalla() {
        int actual = LocalDate.now().getYear();
        int fuera = actual + 2;

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> anioEscolarService.create(request(String.valueOf(fuera), null, LocalDate.of(fuera, 12, 15))));

        assertEquals("Solo se pueden registrar el año " + actual + " y el " + (actual + 1),
                e.getMessage());
    }

    @Test
    void crearUnAnioPasadoFalla() {
        int pasado = LocalDate.now().getYear() - 1;

        assertThrows(IllegalArgumentException.class,
                () -> anioEscolarService.create(request(String.valueOf(pasado), null, LocalDate.of(pasado, 12, 15))));
    }

    @Test
    void actualizarUnAnioFueraDeLaVentanaAcepta() {
        // La ventana acota la creación, no la edición. Si update() también
        // validara, un POR_COMENZAR del año anterior quedaría congelado para
        // siempre en enero, cuando la ventana se corre. Este test es el que
        // protege esa decisión: se pone rojo si alguien agrega el chequeo.
        int pasado = LocalDate.now().getYear() - 1;
        AnioEscolar porComenzar = anio(1, String.valueOf(pasado), AnioEscolarService.ESTADO_POR_COMENZAR,
                LocalDate.of(pasado, 12, 15));
        when(anioEscolarRepository.findByIdAnioAndAccesoNot(anyInt(), any(Acceso.class)))
                .thenReturn(Optional.of(porComenzar));

        AnioEscolarRequest r = request(String.valueOf(pasado), (byte) AnioEscolarService.ESTADO_POR_COMENZAR,
                LocalDate.of(pasado, 12, 20));

        AnioEscolarResponse resultado = anioEscolarService.update(1, r);

        assertEquals(LocalDate.of(pasado, 12, 20), resultado.getFechaFin());
    }

    // ── Crear ──────────────────────────────────────────────────────────────

    @Test
    void crearSinEstadoNacePorComenzar() {
        int siguiente = LocalDate.now().getYear() + 1;
        AnioEscolarResponse resultado = anioEscolarService.create(
                request(String.valueOf(siguiente), null, LocalDate.of(siguiente, 12, 15)));

        assertEquals(AnioEscolarService.ESTADO_POR_COMENZAR, resultado.getEstado());
    }

    @Test
    void crearPorComenzarNoCierraElVigente() {
        int actual = LocalDate.now().getYear();
        int siguiente = actual + 1;
        AnioEscolar vigente = anio(1, String.valueOf(actual), AnioEscolarService.ESTADO_VIGENTE,
                LocalDate.of(actual, 12, 15));
        when(anioEscolarRepository.findByAccesoNot(any(Acceso.class))).thenReturn(List.of(vigente));

        anioEscolarService.create(request(String.valueOf(siguiente), null, LocalDate.of(siguiente, 12, 15)));

        // Este es el caso que motivated el cambio: crear un año futuro no debe
        // tumbar el año que todavía está en curso.
        assertEquals(AnioEscolarService.ESTADO_VIGENTE, vigente.getEstado());
    }

    @Test
    void crearComoVigenteCierraLosDemas() {
        // El año que se crea tiene que ser el siguiente al vigente que se va a
        // cerrar: si no, la ventana de dos años lo rechazaría antes de llegar
        // aquí y el test probaría otra cosa.
        int actual = LocalDate.now().getYear();
        int siguiente = actual + 1;
        AnioEscolar anterior = anio(1, String.valueOf(actual), AnioEscolarService.ESTADO_VIGENTE,
                LocalDate.of(actual, 12, 15));
        when(anioEscolarRepository.findByAccesoNot(any(Acceso.class))).thenReturn(List.of(anterior));

        anioEscolarService.create(request(String.valueOf(actual),
                (byte) AnioEscolarService.ESTADO_VIGENTE, LocalDate.of(actual, 12, 15)));

        assertEquals(AnioEscolarService.ESTADO_CERRADO, anterior.getEstado());
    }

    // ── Barrido por fecha ──────────────────────────────────────────────────

    @Test
    void elBarridoCierraVigenteYPorComenzarVencidos() {
        AnioEscolar vencidoVigente = anio(1, "2024", AnioEscolarService.ESTADO_VIGENTE, LocalDate.of(2024, 12, 15));
        AnioEscolar vencidoPorComenzar = anio(2, "2025", AnioEscolarService.ESTADO_POR_COMENZAR, LocalDate.of(2025, 3, 1));
        when(anioEscolarRepository.findByEstadoInAndFechaFinBeforeAndAccesoNot(
                anyList(), any(LocalDate.class), any(Acceso.class)))
                .thenReturn(List.of(vencidoVigente, vencidoPorComenzar));

        anioEscolarService.getAll();

        assertEquals(AnioEscolarService.ESTADO_CERRADO, vencidoVigente.getEstado());
        assertEquals(AnioEscolarService.ESTADO_CERRADO, vencidoPorComenzar.getEstado());
    }

    @Test
    void elBarridoTomaVigenteYPorComenzar() {
        anioEscolarService.getAll();

        // Si el barrido solo mirara el vigente, editar la fecha de fin de un año
        // por comenzar no serviría para cerrarlo.
        verify(anioEscolarRepository).findByEstadoInAndFechaFinBeforeAndAccesoNot(
                org.mockito.ArgumentMatchers.argThat((List<Byte> lista) ->
                        lista.contains(AnioEscolarService.ESTADO_VIGENTE)
                                && lista.contains(AnioEscolarService.ESTADO_POR_COMENZAR)
                                && !lista.contains(AnioEscolarService.ESTADO_CERRADO)),
                any(LocalDate.class), any(Acceso.class));
    }

    @Test
    void elBarridoNoGuardaSiNoHayVencidos() {
        anioEscolarService.getAll();

        verify(anioEscolarRepository, never()).saveAll(anyList());
    }

    // ── Transiciones ───────────────────────────────────────────────────────

    @Test
    void noSePuedeVolverAPorComenzarUnVigente() {
        AnioEscolar vigente = anio(1, "2026", AnioEscolarService.ESTADO_VIGENTE, LocalDate.of(2026, 12, 15));
        when(anioEscolarRepository.findByIdAnioAndAccesoNot(anyInt(), any(Acceso.class)))
                .thenReturn(Optional.of(vigente));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> anioEscolarService.cambiarEstado(1, (byte) AnioEscolarService.ESTADO_POR_COMENZAR));

        assertEquals("No se puede volver a 'por comenzar' un año que ya está vigente", e.getMessage());
    }

    @Test
    void unAnoCerradoNoCambiaDeEstado() {
        AnioEscolar cerrado = anio(1, "2024", AnioEscolarService.ESTADO_CERRADO, LocalDate.of(2024, 12, 15));
        when(anioEscolarRepository.findByIdAnioAndAccesoNot(anyInt(), any(Acceso.class)))
                .thenReturn(Optional.of(cerrado));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> anioEscolarService.cambiarEstado(1, (byte) AnioEscolarService.ESTADO_VIGENTE));

        assertEquals("El año está cerrado y no admite cambios de estado", e.getMessage());
    }

    @Test
    void activarVigenteCierraElVigenteAnterior() {
        AnioEscolar porComenzar = anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR, LocalDate.of(2027, 12, 15));
        AnioEscolar vigenteActual = anio(1, "2026", AnioEscolarService.ESTADO_VIGENTE, LocalDate.of(2026, 12, 15));
        when(anioEscolarRepository.findByIdAnioAndAccesoNot(anyInt(), any(Acceso.class)))
                .thenReturn(Optional.of(porComenzar));
        when(anioEscolarRepository.findByAccesoNot(any(Acceso.class))).thenReturn(List.of(vigenteActual, porComenzar));

        anioEscolarService.cambiarEstado(2, (byte) AnioEscolarService.ESTADO_VIGENTE);

        assertEquals(AnioEscolarService.ESTADO_VIGENTE, porComenzar.getEstado());
        assertEquals(AnioEscolarService.ESTADO_CERRADO, vigenteActual.getEstado());
    }

    @Test
    void vigenteExigeFechaFin() {
        AnioEscolar porComenzar = anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR, null);
        when(anioEscolarRepository.findByIdAnioAndAccesoNot(anyInt(), any(Acceso.class)))
                .thenReturn(Optional.of(porComenzar));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> anioEscolarService.cambiarEstado(2, (byte) AnioEscolarService.ESTADO_VIGENTE));

        assertEquals("Un año vigente necesita fecha de fin para poder cerrarse solo", e.getMessage());
    }

    @Test
    void crearVigenteSinFechaFinFalla() {
        int siguiente = LocalDate.now().getYear() + 1;
        AnioEscolarRequest r = request(String.valueOf(siguiente),
                (byte) AnioEscolarService.ESTADO_VIGENTE, null);

        assertThrows(IllegalArgumentException.class, () -> anioEscolarService.create(r));
    }

    // ── Cerrado inmutable ──────────────────────────────────────────────────

    @Test
    void unAnoCerradoNoSeEdita() {
        AnioEscolar cerrado = anio(1, "2024", AnioEscolarService.ESTADO_CERRADO, LocalDate.of(2024, 12, 15));
        when(anioEscolarRepository.findByIdAnioAndAccesoNot(anyInt(), any(Acceso.class)))
                .thenReturn(Optional.of(cerrado));

        AnioEscolarRequest r = request("2024", (byte) AnioEscolarService.ESTADO_CERRADO,
                LocalDate.of(2024, 12, 20));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> anioEscolarService.update(1, r));

        assertEquals("El año está cerrado y no se puede editar", e.getMessage());
        // El guard tiene que caer antes de tocar campos: si se modificara el
        // entidad en memoria, la respuesta sería exception pero el estado local
        // quedaría alterado para el resto de la transacción.
        verify(anioEscolarRepository, never()).save(any(AnioEscolar.class));
        assertEquals(LocalDate.of(2024, 12, 15), cerrado.getFechaFin());
    }

    @Test
    void unAnoCerradoTampocoAceptaPutSinEstado() {
        AnioEscolar cerrado = anio(1, "2024", AnioEscolarService.ESTADO_CERRADO, LocalDate.of(2024, 12, 15));
        when(anioEscolarRepository.findByIdAnioAndAccesoNot(anyInt(), any(Acceso.class)))
                .thenReturn(Optional.of(cerrado));
        // Petición parcial: sin estado, validarTransicion ni siquiera se invoca,
        // así que el bloqueo depende del guard y no de la transición.
        AnioEscolarRequest r = request(null, null, LocalDate.of(2025, 1, 31));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> anioEscolarService.update(1, r));

        assertEquals("El año está cerrado y no se puede editar", e.getMessage());
        verify(anioEscolarRepository, never()).save(any(AnioEscolar.class));
    }

    @Test
    void unAnoNoCerradoSiSeEdita() {
        // El guard no puede pasar por rechazo indiscriminado: si rechazara
        // cualquier año, estos dos tests también pasarían.
        AnioEscolar porComenzar = anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR,
                LocalDate.of(2027, 12, 15));
        when(anioEscolarRepository.findByIdAnioAndAccesoNot(anyInt(), any(Acceso.class)))
                .thenReturn(Optional.of(porComenzar));

        AnioEscolarRequest r = request("2027", (byte) AnioEscolarService.ESTADO_POR_COMENZAR,
                LocalDate.of(2027, 12, 20));

        AnioEscolarResponse resultado = anioEscolarService.update(2, r);

        assertEquals(LocalDate.of(2027, 12, 20), resultado.getFechaFin());
        verify(anioEscolarRepository).save(any(AnioEscolar.class));
    }

    @Test
    void unVigenteSiSeEdita() {
        AnioEscolar vigente = anio(1, "2026", AnioEscolarService.ESTADO_VIGENTE, LocalDate.of(2026, 12, 15));
        when(anioEscolarRepository.findByIdAnioAndAccesoNot(anyInt(), any(Acceso.class)))
                .thenReturn(Optional.of(vigente));

        AnioEscolarRequest r = request("2026", (byte) AnioEscolarService.ESTADO_VIGENTE,
                LocalDate.of(2026, 12, 18));

        AnioEscolarResponse resultado = anioEscolarService.update(1, r);

        assertEquals(LocalDate.of(2026, 12, 18), resultado.getFechaFin());
    }

    // ── Borrado ────────────────────────────────────────────────────────────

    @Test
    void eliminarConGradosFalla() {
        AnioEscolar anio = anio(1, "2026", AnioEscolarService.ESTADO_VIGENTE, LocalDate.of(2026, 12, 15));
        when(anioEscolarRepository.findByIdAnioAndAccesoNot(anyInt(), any(Acceso.class)))
                .thenReturn(Optional.of(anio));
        when(gradoSeccionRepository.countByAnioEscolarIdAnioAndAccesoNot(anyInt(), any(Acceso.class)))
                .thenReturn(3L);
        when(asignacionRepository.countByAnioEscolarIdAnioAndAccesoNot(anyInt(), any(Acceso.class)))
                .thenReturn(12L);

        AnioEscolarConGradosException e = assertThrows(AnioEscolarConGradosException.class,
                () -> anioEscolarService.delete(1));

        assertEquals("No se puede eliminar el año 2026 porque tiene 3 grados y 12 asignaciones asociados",
                e.getMessage());
        verify(anioEscolarRepository, never()).delete(any(AnioEscolar.class));
    }

    @Test
    void eliminarSinGradosBorraFisicamente() {
        AnioEscolar anio = anio(1, "2026", AnioEscolarService.ESTADO_POR_COMENZAR, LocalDate.of(2026, 12, 15));
        when(anioEscolarRepository.findByIdAnioAndAccesoNot(anyInt(), any(Acceso.class)))
                .thenReturn(Optional.of(anio));
        when(gradoSeccionRepository.countByAnioEscolarIdAnioAndAccesoNot(anyInt(), any(Acceso.class)))
                .thenReturn(0L);
        when(asignacionRepository.countByAnioEscolarIdAnioAndAccesoNot(anyInt(), any(Acceso.class)))
                .thenReturn(0L);

        anioEscolarService.delete(1);

        // Borrado fisico, no logico: anio tiene unique=true, asi que si la fila
        // solo se marcara eliminada seguiria ocupando el año y no se podria
        // volver a crearlo.
        verify(anioEscolarRepository).delete(anio);
        verify(anioEscolarRepository, never()).save(any(AnioEscolar.class));
    }

    @Test
    void eliminarConUnSoloGradoEnSingular() {
        AnioEscolar anio = anio(1, "2026", AnioEscolarService.ESTADO_VIGENTE, LocalDate.of(2026, 12, 15));
        when(anioEscolarRepository.findByIdAnioAndAccesoNot(anyInt(), any(Acceso.class)))
                .thenReturn(Optional.of(anio));
        when(gradoSeccionRepository.countByAnioEscolarIdAnioAndAccesoNot(anyInt(), any(Acceso.class)))
                .thenReturn(1L);
        when(asignacionRepository.countByAnioEscolarIdAnioAndAccesoNot(anyInt(), any(Acceso.class)))
                .thenReturn(0L);

        AnioEscolarConGradosException e = assertThrows(AnioEscolarConGradosException.class,
                () -> anioEscolarService.delete(1));

        assertEquals("No se puede eliminar el año 2026 porque tiene 1 grado asociados", e.getMessage());
    }
}