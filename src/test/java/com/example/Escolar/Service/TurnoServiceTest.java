package com.example.Escolar.Service;

import com.example.Escolar.Dto.TurnoRequest;
import com.example.Escolar.Dto.TurnoResponse;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Turno;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TurnoServiceTest {

    private static final Long ID_ACTIVO = 1L;
    private static final Long ID_ELIMINADO = 2L;

    @Mock
    private TurnoRepository turnoRepository;
    @Mock
    private AccesoRepository accesoRepository;
    @InjectMocks
    private TurnoService turnoService;

    private Acceso activo;
    private Acceso eliminado;

    @BeforeEach
    void setUp() {
        activo = new Acceso();
        activo.setIdAcceso(ID_ACTIVO);
        eliminado = new Acceso();
        eliminado.setIdAcceso(ID_ELIMINADO);

        when(accesoRepository.findById(ID_ACTIVO)).thenReturn(Optional.of(activo));
        when(accesoRepository.findById(ID_ELIMINADO)).thenReturn(Optional.of(eliminado));
        when(turnoRepository.findByAccesoNot(eliminado)).thenReturn(new ArrayList<>());
        when(turnoRepository.save(any(Turno.class))).thenAnswer(inv -> inv.getArgument(0));
        when(turnoRepository.findByNombreAndAccesoNot(any(), any())).thenReturn(Optional.empty());
        // Cualquier turno guardado es localizable: a estos tests les interesa la
        // validacion, no que el registro exista de verdad.
        when(turnoRepository.findByIdTurnoAndAccesoNot(any(), any())).thenAnswer(inv ->
                Optional.of(turno(inv.getArgument(0), "Mañana", "07:00", "12:30")));
    }

    private Turno turno(Integer id, String nombre, String entrada, String salida) {
        Turno t = new Turno();
        t.setIdTurno(id);
        t.setNombre(nombre);
        t.setHoraEntrada(LocalTime.parse(entrada));
        t.setHoraEntradaLimite(LocalTime.parse(entrada));
        t.setHoraFaltaLimite(LocalTime.parse(entrada));
        t.setHoraSalida(LocalTime.parse(salida));
        t.setAcceso(activo);
        return t;
    }

    private TurnoRequest request(String nombre, String entrada, String limite, String falta, String salida) {
        TurnoRequest r = new TurnoRequest();
        r.setNombre(nombre);
        r.setHoraEntrada(LocalTime.parse(entrada));
        r.setHoraEntradaLimite(LocalTime.parse(limite));
        r.setHoraFaltaLimite(LocalTime.parse(falta));
        r.setHoraSalida(LocalTime.parse(salida));
        r.setAccesoId(ID_ACTIVO);
        return r;
    }

    // ---- Orden de las cuatro horas ----

    @Test
    void aceptaHorasEnOrden() {
        TurnoResponse res = turnoService.create(request("Mañana", "07:00", "07:20", "07:40", "12:30"));
        assertEquals("Mañana", res.getNombre());
    }

    @Test
    void rechazaLimiteDeEntradaAnteriorALaEntrada() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> turnoService.create(request("Mañana", "07:00", "06:59", "07:40", "12:30")));
        assertTrue(ex.getMessage().contains("limite de puntualidad"));
    }

    @Test
    void rechazaLimiteDeFaltaAnteriorAlLimiteDeEntrada() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> turnoService.create(request("Mañana", "07:00", "07:20", "07:10", "12:30")));
        assertTrue(ex.getMessage().contains("limite de tardanza"));
    }

    @Test
    void rechazaSalidaAnteriorAlLimiteDeFalta() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> turnoService.create(request("Mañana", "07:00", "07:20", "07:40", "07:30")));
        assertTrue(ex.getMessage().contains("hora de salida"));
    }

    @Test
    void rechazaHorasRepetidasDentroDelMismoTurno() {
        assertThrows(IllegalArgumentException.class,
                () -> turnoService.create(request("Mañana", "07:00", "07:00", "07:40", "12:30")));
    }

    // ---- Solapamiento entre turnos ----

    @Test
    void elPrimerTurnoDeLaJornadaPaseAunqueNoHayaConQuienCompararse() {
        assertDoesNotThrow(() -> turnoService.create(request("Mañana", "06:00", "06:20", "06:40", "12:30")));
    }

    @Test
    void elEmpateDeLaCadenaEstaPermitido() {
        when(turnoRepository.findByAccesoNot(eliminado))
                .thenReturn(List.of(turno(1, "Mañana", "07:00", "12:30")));
        // Tarde entra justo cuando sale la manana: es el cambio de turno.
        assertDoesNotThrow(() -> turnoService.create(request("Tarde", "12:30", "12:50", "13:10", "17:30")));
    }

    @Test
    void rechazaTurnoQueInvadeLaSalidaDelAnterior() {
        when(turnoRepository.findByAccesoNot(eliminado))
                .thenReturn(List.of(turno(1, "Mañana", "07:00", "12:30")));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> turnoService.create(request("Tarde", "12:00", "12:50", "13:10", "17:30")));
        assertTrue(ex.getMessage().contains("Mañana"));
        assertTrue(ex.getMessage().contains("12:30"));
    }

    @Test
    void noComparaConElTurnoQueLoSigue() {
        when(turnoRepository.findByAccesoNot(eliminado))
                .thenReturn(List.of(turno(2, "Tarde", "12:30", "17:30")));
        // Editar la manana no falla aunque exista una tarde que hoy se pisa con
        // ella: cada turno se valida solo contra el anterior.
        assertDoesNotThrow(() -> turnoService.update(1, request("Mañana", "07:00", "07:20", "07:40", "13:00")));
    }

    @Test
    void alEditarSeIgnoraASiMismo() {
        // El unico turno de la lista es el que se esta editando. Si no se
        // excluyera de la cadena, compararia sus horas viejas contra las nuevas.
        when(turnoRepository.findByAccesoNot(eliminado))
                .thenReturn(List.of(turno(1, "Mañana", "07:00", "12:30")));
        assertDoesNotThrow(() -> turnoService.update(1, request("Mañana", "07:00", "07:20", "07:40", "12:30")));
    }

    @Test
    void alEditarSeValidaConLasHorasNuevasYNoConLasGuardadas() {
        when(turnoRepository.findByAccesoNot(eliminado))
                .thenReturn(List.of(turno(1, "Mañana", "07:00", "12:30")));
        when(turnoRepository.findByIdTurnoAndAccesoNot(any(), any()))
                .thenReturn(Optional.of(turno(1, "Mañana", "07:00", "12:30")));
        // Si se usara la hora guardada (07:00) no habria conflicto con la manana
        // que termina 12:30, pero con la nueva (12:00) si.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> turnoService.update(2, request("Tarde", "12:00", "12:50", "13:10", "17:30")));
        assertTrue(ex.getMessage().contains("Mañana"));
    }

    @Test
    void elDesempatePorHoraSeResuelveConElId() {
        // Dos turnos que empiezan a la misma hora: el de id menor va primero y
        // el de id mayor es el que se valida contra el.
        when(turnoRepository.findByAccesoNot(eliminado))
                .thenReturn(List.of(turno(2, "Tarde", "12:30", "17:30"), turno(1, "Mañana", "12:30", "12:45")));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> turnoService.update(2, request("Tarde", "12:30", "12:50", "13:10", "17:30")));
        assertTrue(ex.getMessage().contains("Mañana"));
    }

    // ---- Lo que ya existia ----

    @Test
    void rechazaNombreDuplicado() {
        when(turnoRepository.findByNombreAndAccesoNot(any(), any()))
                .thenReturn(Optional.of(turno(1, "Mañana", "07:00", "12:30")));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> turnoService.create(request("Mañana", "07:00", "07:20", "07:40", "12:30")));
        assertTrue(ex.getMessage().contains("Ya existe un turno con ese nombre"));
    }

    @Test
    void elNombreDuplicadoSeIgnoraAlEditarElMismoTurno() {
        Turno existente = turno(1, "Mañana", "07:00", "12:30");
        when(turnoRepository.findByIdTurnoAndAccesoNot(any(), any())).thenReturn(Optional.of(existente));
        when(turnoRepository.findByNombreAndAccesoNot(any(), any())).thenReturn(Optional.of(existente));
        assertDoesNotThrow(() -> turnoService.update(1, request("Mañana", "07:00", "07:20", "07:40", "12:30")));
    }

    // ---- Los horarios que siembra el instalador ----

    @Test
    void losTurnosSembradosNoSePisanEntreSi() {
        Turno manana = turno(1, "Mañana", "07:00", "12:30");
        Turno tarde = turno(2, "Tarde", "12:30", "17:30");
        when(turnoRepository.findByAccesoNot(eliminado)).thenReturn(List.of(manana, tarde));

        assertDoesNotThrow(() -> turnoService.update(1, request("Mañana", "07:00", "07:20", "07:40", "12:30")));
        assertDoesNotThrow(() -> turnoService.update(2, request("Tarde", "12:30", "12:50", "13:10", "17:30")));
    }
}