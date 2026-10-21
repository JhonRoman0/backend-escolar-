package com.example.Escolar.Service;

import com.example.Escolar.Dto.RecreoRequest;
import com.example.Escolar.Dto.RecreoUpdateRequest;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.Asignacion;
import com.example.Escolar.Model.Curso;
import com.example.Escolar.Model.Grado;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.HorarioClase;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Model.Recreo;
import com.example.Escolar.Model.Seccion;
import com.example.Escolar.Model.Turno;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.GradoRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.HorarioClaseRepository;
import com.example.Escolar.Repository.NivelRepository;
import com.example.Escolar.Repository.RecreoRepository;
import com.example.Escolar.Repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecreoServiceTest {

    @Mock
    private RecreoRepository recreoRepository;
    @Mock
    private AnioEscolarRepository anioEscolarRepository;
    @Mock
    private NivelRepository nivelRepository;
    @Mock
    private TurnoRepository turnoRepository;
    @Mock
    private GradoRepository gradoRepository;
    @Mock
    private GradoSeccionRepository gradoSeccionRepository;
    @Mock
    private HorarioClaseRepository horarioClaseRepository;
    @Mock
    private AccesoRepository accesoRepository;
    @InjectMocks
    private RecreoService recreoService;

    private Acceso activo;
    private AnioEscolar anio;
    private Nivel primaria;
    private Turno manana;
    private Grado primero;
    private Grado segundo;
    private GradoSeccion primeroA;

    @BeforeEach
    void setUp() {
        activo = acceso(AccesoConstants.ACTIVO);
        Acceso eliminado = acceso(AccesoConstants.ELIMINADO);
        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(activo));
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));

        anio = new AnioEscolar();
        anio.setIdAnio(2);
        anio.setAnio("2027");
        anio.setEstado(AnioEscolarService.ESTADO_POR_COMENZAR);
        when(anioEscolarRepository.findByIdAnioAndAccesoNot(eq(2), any())).thenReturn(Optional.of(anio));

        primaria = new Nivel();
        primaria.setIdNivel(2);
        primaria.setNombre("Primaria");
        when(nivelRepository.findByIdNivelAndAccesoNot(eq(2), any())).thenReturn(Optional.of(primaria));

        manana = new Turno();
        manana.setIdTurno(1);
        manana.setNombre("Mañana");
        manana.setHoraEntrada(LocalTime.of(7, 0));
        manana.setHoraSalida(LocalTime.of(12, 30));
        when(turnoRepository.findByIdTurnoAndAccesoNot(eq(1), any())).thenReturn(Optional.of(manana));

        primero = grado(10, "1.° grado");
        segundo = grado(11, "2.° grado");
        primeroA = seccion(50, primero);
        when(gradoSeccionRepository.findByGradoIdGradoAndTurnoIdTurnoAndAnioEscolarIdAnioAndAccesoNot(
                anyInt(), eq(1), eq(2), any())).thenAnswer(inv -> List.of(seccion(inv.getArgument(0), null)));
        when(gradoSeccionRepository.findByGradoIdGradoAndTurnoIdTurnoAndAnioEscolarIdAnioAndAccesoNot(
                eq(10), eq(1), eq(2), any())).thenReturn(List.of(primeroA));
        when(recreoRepository.findByAnioEscolarIdAnioAndTurnoIdTurnoAndGradoIdGradoAndAccesoNot(
                anyInt(), anyInt(), anyInt(), any())).thenReturn(new ArrayList<>());
        when(recreoRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(recreoRepository.save(any(Recreo.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Acceso acceso(Long id) {
        Acceso a = new Acceso();
        a.setIdAcceso(id);
        return a;
    }

    private Grado grado(int id, String nombre) {
        Grado g = new Grado();
        g.setIdGrado(id);
        g.setNombre(nombre);
        g.setNivel(primaria);
        when(gradoRepository.findByIdGradoAndAccesoNot(eq(id), any())).thenReturn(Optional.of(g));
        return g;
    }

    private GradoSeccion seccion(int id, Grado grado) {
        GradoSeccion gs = new GradoSeccion();
        gs.setIdGradoSeccion(id);
        gs.setGrado(grado);
        Seccion s = new Seccion();
        s.setNombre("A");
        gs.setSeccion(s);
        return gs;
    }

    private RecreoRequest.Franja franja(String inicio, String fin) {
        RecreoRequest.Franja f = new RecreoRequest.Franja();
        f.setHoraInicio(LocalTime.parse(inicio));
        f.setHoraFin(LocalTime.parse(fin));
        return f;
    }

    private RecreoRequest request(List<Integer> grados, List<Byte> dias, RecreoRequest.Franja... franjas) {
        RecreoRequest r = new RecreoRequest();
        r.setIdAnio(2);
        r.setIdNivel(2);
        r.setIdTurno(1);
        r.setIdGrados(grados);
        r.setDias(dias);
        r.setFranjas(List.of(franjas));
        return r;
    }

    private Recreo recreo(int id, Grado grado, int dia, String inicio, String fin) {
        Recreo r = new Recreo();
        r.setIdRecreo(id);
        r.setAnioEscolar(anio);
        r.setNivel(primaria);
        r.setTurno(manana);
        r.setGrado(grado);
        r.setDiaSemana((byte) dia);
        r.setHoraInicio(LocalTime.parse(inicio));
        r.setHoraFin(LocalTime.parse(fin));
        r.setAcceso(activo);
        when(recreoRepository.findByIdRecreoAndAccesoNot(eq(id), any())).thenReturn(Optional.of(r));
        return r;
    }

    /** Una clase de 1.° A el dia y la hora indicados. */
    private void claseDePrimeroA(int dia, String inicio, String fin) {
        Curso curso = new Curso();
        curso.setNombre("Matemática");
        Asignacion asignacion = new Asignacion();
        asignacion.setIdAsignacion(90);
        asignacion.setCurso(curso);
        asignacion.setGradoSeccion(primeroA);
        asignacion.setAcceso(activo);
        HorarioClase clase = new HorarioClase();
        clase.setAsignacion(asignacion);
        clase.setDiaSemana((byte) dia);
        clase.setHoraInicio(LocalTime.parse(inicio));
        clase.setHoraFin(LocalTime.parse(fin));
        when(horarioClaseRepository.findByAsignacionGradoSeccionIdGradoSeccionInAndAccesoNot(anyCollection(), any()))
                .thenReturn(List.of(clase));
    }

    private static final List<Byte> LUNES_A_VIERNES = List.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);

    // ── Crear ──────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void creaUnRecreoPorCadaGradoDiaYFranja() {
        recreoService.create(request(List.of(10, 11), LUNES_A_VIERNES,
                franja("09:30", "09:45"), franja("11:00", "11:10")));

        ArgumentCaptor<List<Recreo>> captor = ArgumentCaptor.forClass(List.class);
        verify(recreoRepository).saveAll(captor.capture());
        List<Recreo> guardados = captor.getValue();
        assertEquals(2 * 5 * 2, guardados.size());
        assertTrue(guardados.stream().allMatch(r -> r.getAnioEscolar() == anio && r.getTurno() == manana));
        assertEquals(10, guardados.stream().filter(r -> r.getGrado() == segundo).count());
    }

    @Test
    void elSabadoSePuedeElegir() {
        assertDoesNotThrow(() -> recreoService.create(request(List.of(10), List.of((byte) 6), franja("09:30", "09:45"))));
    }

    @Test
    void rechazaElDomingo() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> recreoService.create(request(List.of(10), List.of((byte) 7), franja("09:30", "09:45"))));
        assertTrue(ex.getMessage().contains("lunes a sábado"));
    }

    @Test
    void rechazaUnaHoraFueraDelTurno() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> recreoService.create(request(List.of(10), LUNES_A_VIERNES, franja("12:20", "12:40"))));
        assertTrue(ex.getMessage().contains("fuera del turno Mañana"));
        verify(recreoRepository, never()).saveAll(anyList());
    }

    @Test
    void rechazaUnFinQueNoEsPosteriorAlInicio() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> recreoService.create(request(List.of(10), LUNES_A_VIERNES, franja("10:00", "10:00"))));
        assertTrue(ex.getMessage().contains("posterior"));
    }

    @Test
    void rechazaRecreosDelFormularioQueSeCruzan() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> recreoService.create(request(List.of(10), LUNES_A_VIERNES,
                        franja("10:00", "10:20"), franja("10:15", "10:30"))));
        assertTrue(ex.getMessage().contains("se cruzan"));
    }

    @Test
    void rechazaElCruceConUnRecreoYaGuardado() {
        Recreo guardado = recreo(1, primero, 1, "10:00", "10:15");
        when(recreoRepository.findByAnioEscolarIdAnioAndTurnoIdTurnoAndGradoIdGradoAndAccesoNot(
                eq(2), eq(1), eq(10), any())).thenReturn(List.of(guardado));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> recreoService.create(request(List.of(10), List.of((byte) 1), franja("10:10", "10:20"))));
        assertTrue(ex.getMessage().contains("ya tiene recreo el lunes"));
    }

    @Test
    void rechazaElCruceConUnaClaseYaAsignada() {
        claseDePrimeroA(1, "10:00", "11:00");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> recreoService.create(request(List.of(10), List.of((byte) 1), franja("10:30", "10:45"))));
        assertTrue(ex.getMessage().contains("choca con Matemática"));
    }

    @Test
    void rechazaUnGradoSinSeccionesEnEseTurno() {
        when(gradoSeccionRepository.findByGradoIdGradoAndTurnoIdTurnoAndAnioEscolarIdAnioAndAccesoNot(
                eq(11), eq(1), eq(2), any())).thenReturn(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> recreoService.create(request(List.of(11), LUNES_A_VIERNES, franja("10:00", "10:15"))));
        assertTrue(ex.getMessage().contains("no tiene secciones"));
    }

    // ── Estado del año ─────────────────────────────────────────────────────

    @Test
    void unAnioVigenteAdmiteLaConfiguracionInicialDeUnGrado() {
        anio.setEstado(AnioEscolarService.ESTADO_VIGENTE);
        assertDoesNotThrow(() -> recreoService.create(request(List.of(10), LUNES_A_VIERNES, franja("10:00", "10:15"))));
    }

    @Test
    void unAnioVigenteNoAgregaRecreosAUnGradoQueYaLosTiene() {
        anio.setEstado(AnioEscolarService.ESTADO_VIGENTE);
        Recreo guardado = recreo(1, primero, 1, "10:00", "10:15");
        when(recreoRepository.findByAnioEscolarIdAnioAndTurnoIdTurnoAndGradoIdGradoAndAccesoNot(
                eq(2), eq(1), eq(10), any())).thenReturn(List.of(guardado));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> recreoService.create(request(List.of(10), List.of((byte) 2), franja("11:00", "11:15"))));
        assertTrue(ex.getMessage().contains("ya no se pueden crear recreos"));
    }

    @Test
    void unAnioCerradoNoAdmiteRecreos() {
        anio.setEstado(AnioEscolarService.ESTADO_CERRADO);
        assertThrows(IllegalArgumentException.class,
                () -> recreoService.create(request(List.of(10), LUNES_A_VIERNES, franja("10:00", "10:15"))));
    }

    @Test
    void enUnAnioVigenteNoSeEditaNiSeEliminaUnRecreo() {
        anio.setEstado(AnioEscolarService.ESTADO_VIGENTE);
        recreo(1, primero, 1, "10:00", "10:15");
        RecreoUpdateRequest cambio = new RecreoUpdateRequest();
        cambio.setDiaSemana((byte) 1);
        cambio.setHoraInicio(LocalTime.of(10, 30));
        cambio.setHoraFin(LocalTime.of(10, 45));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> recreoService.update(1, cambio));
        assertTrue(ex.getMessage().contains("ya no se pueden editar ni eliminar"));
        assertThrows(IllegalArgumentException.class, () -> recreoService.delete(1));
    }

    // ── Eliminar ───────────────────────────────────────────────────────────

    @Test
    void noSeEliminaElUnicoRecreoDeUnDiaConClases() {
        Recreo lunes = recreo(1, primero, 1, "10:00", "10:15");
        when(recreoRepository.findByAnioEscolarIdAnioAndTurnoIdTurnoAndGradoIdGradoAndAccesoNot(
                eq(2), eq(1), eq(10), any())).thenReturn(List.of(lunes));
        claseDePrimeroA(1, "08:00", "09:00");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> recreoService.delete(1));
        assertTrue(ex.getMessage().contains("único recreo"));
    }

    @Test
    void siQuedaOtroRecreoEseDiaSePuedeEliminar() {
        Recreo primeroDelLunes = recreo(1, primero, 1, "10:00", "10:15");
        Recreo segundoDelLunes = recreo(2, primero, 1, "11:30", "11:40");
        when(recreoRepository.findByAnioEscolarIdAnioAndTurnoIdTurnoAndGradoIdGradoAndAccesoNot(
                eq(2), eq(1), eq(10), any())).thenReturn(List.of(primeroDelLunes, segundoDelLunes));
        claseDePrimeroA(1, "08:00", "09:00");

        recreoService.delete(1);

        assertEquals(AccesoConstants.ELIMINADO, primeroDelLunes.getAcceso().getIdAcceso());
    }
}
