package com.example.Escolar.Service;

import com.example.Escolar.Dto.AsignarSustitutoRequest;
import com.example.Escolar.Dto.CambioDocenteRequest;
import com.example.Escolar.Dto.EditarCambioVigenteRequest;
import com.example.Escolar.Dto.ResolverIncidenciaRequest;
import com.example.Escolar.Dto.CambioDocenteResponse;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.Asignacion;
import com.example.Escolar.Model.CambioDocente;
import com.example.Escolar.Model.CambioDocenteAsignacion;
import com.example.Escolar.Model.Curso;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.EstadoCambioDocente;
import com.example.Escolar.Model.Grado;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.HorarioClase;
import com.example.Escolar.Model.MotivoCambioDocente;
import com.example.Escolar.Model.OrigenOperacion;
import com.example.Escolar.Model.Seccion;
import com.example.Escolar.Model.TipoCambioDocente;
import com.example.Escolar.Model.SuspensionDocente;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.CambioDocenteAsignacionRepository;
import com.example.Escolar.Repository.CambioDocenteRepository;
import com.example.Escolar.Repository.DocenteRepository;
import com.example.Escolar.Repository.HorarioClaseRepository;
import com.example.Escolar.Repository.MotivoCambioDocenteRepository;
import com.example.Escolar.Repository.SuspensionDocenteRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Security.ConcesionesService;
import com.example.Escolar.Util.FechaColegio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CambioDocenteServiceTest {

    @Mock
    private CambioDocenteRepository cambioRepository;
    @Mock
    private CambioDocenteAsignacionRepository detalleRepository;
    @Mock
    private MotivoCambioDocenteRepository motivoRepository;
    @Mock
    private AsignacionRepository asignacionRepository;
    @Mock
    private DocenteRepository docenteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private SuspensionDocenteRepository suspensionRepository;
    @Mock
    private HorarioClaseRepository horarioClaseRepository;
    @Mock
    private AccesoRepository accesoRepository;
    @Mock
    private AccesoContextoService contexto;
    @Mock
    private ConcesionesService concesionesService;
    @Mock
    private BloqueoCalificaciones bloqueo;
    @InjectMocks
    private CambioDocenteService service;

    private Acceso activo;
    private Acceso inactivo;
    private Acceso eliminado;
    private AnioEscolar anio;
    private Docente titular;
    private Docente sustituto;
    private Asignacion asignacion;
    private MotivoCambioDocente motivo;
    private Usuario usuario;
    private LocalDate hoy;
    private final List<CambioDocenteAsignacion> detallesGuardados = new ArrayList<>();

    @BeforeEach
    void setUp() {
        detallesGuardados.clear();
        hoy = FechaColegio.hoy();
        activo = acceso((long) AccesoConstants.ACTIVO);
        inactivo = acceso((long) AccesoConstants.INACTIVO);
        eliminado = acceso((long) AccesoConstants.ELIMINADO);

        usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setNombre("Ana");
        usuario.setApellidoPat("Pérez");
        usuario.setApellidoMat("López");

        titular = docente(10, "Titular");
        sustituto = docente(11, "Sustituto");

        anio = new AnioEscolar();
        anio.setIdAnio(5);
        anio.setAnio("2026");
        anio.setEstado(AnioEscolarService.ESTADO_VIGENTE);

        asignacion = new Asignacion();
        asignacion.setIdAsignacion(100);
        asignacion.setDocente(titular);
        asignacion.setAnioEscolar(anio);
        asignacion.setAcceso(activo);

        motivo = new MotivoCambioDocente();
        motivo.setIdMotivoCambioDocente(1);
        motivo.setNombre("Licencia");
        motivo.setAplicaTemporal(true);
        motivo.setAplicaDefinitivo(true);
        motivo.setAcceso(activo);

        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(activo));
        when(accesoRepository.findById(AccesoConstants.INACTIVO)).thenReturn(Optional.of(inactivo));
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        when(contexto.idUsuarioAutenticado()).thenReturn(1);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(motivoRepository.findByIdMotivoCambioDocenteAndAcceso(1, activo)).thenReturn(Optional.of(motivo));
        when(asignacionRepository.findAllById(List.of(100))).thenReturn(List.of(asignacion));
        when(docenteRepository.findById(11)).thenReturn(Optional.of(sustituto));
        when(suspensionRepository.existsByDocenteAndEstadoInAndAccesoNot(any(), any(), any())).thenReturn(false);
        when(detalleRepository.findByAsignacionIdAsignacionInAndDevueltaFalseAndAccesoNot(any(), eq(eliminado)))
                .thenReturn(List.of());
        when(cambioRepository.save(any())).thenAnswer(inv -> {
            CambioDocente guardado = inv.getArgument(0);
            if (guardado.getIdCambio() == null) {
                guardado.setIdCambio(7);
            }
            return guardado;
        });
        when(detalleRepository.save(any())).thenAnswer(inv -> {
            CambioDocenteAsignacion detalle = inv.getArgument(0);
            if (!detallesGuardados.contains(detalle)) {
                detallesGuardados.add(detalle);
            }
            return detalle;
        });
        when(detalleRepository.findByCambioIdCambioAndAccesoNot(anyInt(), any())).thenAnswer(inv -> {
            Integer idCambio = inv.getArgument(0);
            return detallesGuardados.stream()
                    .filter(d -> d.getCambio() != null && idCambio.equals(d.getCambio().getIdCambio()))
                    .filter(d -> !eliminado.equals(d.getAcceso()))
                    .toList();
        });
        when(asignacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void coberturaDeSuspensionTransfiereYDevuelveSoloLaAsignacionDelTitular() {
        SuspensionDocente suspension = new SuspensionDocente();
        suspension.setIdSuspension(17);
        suspension.setDocente(titular);
        suspension.setSustituto(sustituto);
        suspension.setFechaInicio(hoy);
        suspension.setMotivo("Licencia");

        service.prepararCobertura(suspension, hoy);
        ArgumentCaptor<CambioDocente> captor = ArgumentCaptor.forClass(CambioDocente.class);
        verify(cambioRepository).save(captor.capture());
        CambioDocente cobertura = captor.getValue();
        assertSame(suspension, cobertura.getSuspension());
        assertSame(titular, asignacion.getDocente());

        Asignacion propia = new Asignacion();
        propia.setIdAsignacion(101);
        propia.setDocente(sustituto);
        when(cambioRepository.findBySuspensionIdSuspension(17)).thenReturn(Optional.of(cobertura));
        when(asignacionRepository.findByDocenteIdDocenteAndAnioEscolarEstadoAndAccesoNot(
                titular.getIdDocente(), AnioEscolarService.ESTADO_VIGENTE, eliminado)).thenReturn(List.of(asignacion));

        service.activarCobertura(suspension, hoy, false);
        assertSame(sustituto, asignacion.getDocente());
        assertEquals(EstadoCambioDocente.VIGENTE, cobertura.getEstado());

        assertTrue(service.cerrarCobertura(suspension, hoy.plusDays(1), false));
        assertSame(titular, asignacion.getDocente());
        assertSame(sustituto, propia.getDocente());
        assertEquals(EstadoCambioDocente.FINALIZADO, cobertura.getEstado());
        assertEquals(hoy.plusDays(1), detallesGuardados.get(0).getFechaDevolucion());
        verify(asignacionRepository, never()).save(propia);
    }

    @Test
    void creaDefinitivoYTransfiereAlSustituto() {
        CambioDocenteResponse respuesta = service.create(new CambioDocenteRequest(
                TipoCambioDocente.DEFINITIVO, hoy, null, 1, "Traslado", List.of(100), 11));

        assertEquals(EstadoCambioDocente.APLICADO, respuesta.getEstado());
        assertEquals(11, respuesta.getIdDocenteNuevo());
        assertEquals(OrigenOperacion.MANUAL, respuesta.getOrigenRegistro());
        assertEquals(1, respuesta.getIdUsuarioRegistro());
        assertEquals(sustituto, asignacion.getDocente());
        ArgumentCaptor<CambioDocenteAsignacion> captor = ArgumentCaptor.forClass(CambioDocenteAsignacion.class);
        verify(detalleRepository, times(2)).save(captor.capture());
        CambioDocenteAsignacion detalle = captor.getAllValues().get(1);
        assertFalse(detalle.getSinCobertura());
        assertEquals(OrigenOperacion.MANUAL, detalle.getOrigenTransferencia());
        assertEquals(hoy, detalle.getFechaTransferencia());
        verify(asignacionRepository, times(1)).findAllById(List.of(100));
        verify(asignacionRepository, never()).findById(100);
    }

    @Test
    void creaTemporalConSustitutoYTransfiere() {
        CambioDocenteResponse respuesta = service.create(new CambioDocenteRequest(
                TipoCambioDocente.TEMPORAL, hoy, hoy.plusDays(7), 1, "Licencia", List.of(100), 11));

        assertEquals(EstadoCambioDocente.VIGENTE, respuesta.getEstado());
        assertEquals(11, respuesta.getIdDocenteNuevo());
        assertEquals(sustituto, asignacion.getDocente());
        ArgumentCaptor<CambioDocenteAsignacion> captor = ArgumentCaptor.forClass(CambioDocenteAsignacion.class);
        verify(detalleRepository, times(2)).save(captor.capture());
        assertFalse(captor.getAllValues().get(1).getSinCobertura());
    }

    @Test
    void rechazaCambioTemporalSinSustituto() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(new CambioDocenteRequest(
                        TipoCambioDocente.TEMPORAL, hoy, hoy.plusDays(7), 1, "Licencia", List.of(100), null)));

        assertEquals("El docente sustituto es obligatorio", ex.getMessage());
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    void rechazaAsignacionInactiva() {
        when(asignacionRepository.findAllById(List.of(100))).thenReturn(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(new CambioDocenteRequest(
                        TipoCambioDocente.TEMPORAL, hoy, hoy.plusDays(7), 1, "Licencia", List.of(100), 11)));

        assertEquals("La asignación #100 no está activa", ex.getMessage());
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    void aceptaMotivoActivoYCompatible() {
        when(motivoRepository.findByIdMotivoCambioDocenteAndAcceso(1, activo))
                .thenReturn(Optional.of(motivo));

        assertDoesNotThrow(() -> service.create(new CambioDocenteRequest(
                TipoCambioDocente.TEMPORAL, hoy, hoy.plusDays(1), 1, "Licencia", List.of(100), 11)));
    }

    @Test
    void rechazaMotivoInactivo() {
        motivo.setAcceso(inactivo);
        when(motivoRepository.findByIdMotivoCambioDocenteAndAccesoNot(1, eliminado))
                .thenReturn(Optional.of(motivo));
        when(motivoRepository.findByIdMotivoCambioDocenteAndAcceso(1, activo))
                .thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(new CambioDocenteRequest(
                        TipoCambioDocente.TEMPORAL, hoy, hoy.plusDays(1), 1, "Licencia", List.of(100), 11)));

        assertTrue(ex.getMessage().contains("El motivo de cambio no está activo"));
    }

    @Test
    void rechazaMotivoEliminado() {
        motivo.setAcceso(eliminado);
        when(motivoRepository.findByIdMotivoCambioDocenteAndAccesoNot(1, eliminado))
                .thenReturn(Optional.empty());
        when(motivoRepository.findByIdMotivoCambioDocenteAndAcceso(1, activo))
                .thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(new CambioDocenteRequest(
                        TipoCambioDocente.TEMPORAL, hoy, hoy.plusDays(1), 1, "Licencia", List.of(100), 11)));

        assertTrue(ex.getMessage().contains("El motivo de cambio no está activo"));
    }

    @Test
    void rechazaMotivoIncompatibleConElTipo() {
        motivo.setAplicaTemporal(false);
        when(motivoRepository.findByIdMotivoCambioDocenteAndAcceso(1, activo))
                .thenReturn(Optional.of(motivo));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(new CambioDocenteRequest(
                        TipoCambioDocente.TEMPORAL, hoy, hoy.plusDays(1), 1, "Licencia", List.of(100), 11)));

        assertTrue(ex.getMessage().contains("El motivo no aplica para este tipo de cambio"));
    }

    @Test
    void asignaSustitutoPosteriormente() {
        CambioDocente cambio = cambio(EstadoCambioDocente.VIGENTE, TipoCambioDocente.TEMPORAL);
        CambioDocenteAsignacion detalle = detalle(cambio, titular, null, true);
        when(cambioRepository.findById(7)).thenReturn(Optional.of(cambio));
        when(detalleRepository.findByCambioIdCambioAndAccesoNot(7, eliminado)).thenReturn(List.of(detalle));

        CambioDocenteResponse respuesta = service.asignarSustituto(7, new AsignarSustitutoRequest(11));

        assertEquals(11, respuesta.getIdDocenteNuevo());
        assertEquals(sustituto, asignacion.getDocente());
        assertFalse(detalle.getSinCobertura());
        assertEquals(sustituto, detalle.getDocenteHacia());
        assertEquals(OrigenOperacion.MANUAL, detalle.getOrigenTransferencia());
    }

    @Test
    void finalizaAutomaticoYDevuelveAlTitular() {
        CambioDocente cambio = cambio(EstadoCambioDocente.VIGENTE, TipoCambioDocente.TEMPORAL);
        cambio.setFechaFin(hoy.minusDays(1));
        cambio.setDocenteNuevo(sustituto);
        asignacion.setDocente(sustituto);
        CambioDocenteAsignacion detalle = detalle(cambio, titular, sustituto, false);
        when(cambioRepository.findById(7)).thenReturn(Optional.of(cambio));
        when(detalleRepository.findByCambioIdCambioAndAccesoNot(7, eliminado)).thenReturn(List.of(detalle));

        service.finalizarAutomatico(7, hoy);

        assertEquals(EstadoCambioDocente.FINALIZADO, cambio.getEstado());
        assertEquals(titular, asignacion.getDocente());
        assertTrue(detalle.getDevuelta());
        assertEquals(OrigenOperacion.AUTOMATICO, detalle.getOrigenDevolucion());
        verify(cambioRepository).save(cambio);
    }

    @Test
    void conAnioCerradoNoDevuelveYRegistraIncidencia() {
        anio.setEstado(AnioEscolarService.ESTADO_CERRADO);
        CambioDocente cambio = cambio(EstadoCambioDocente.VIGENTE, TipoCambioDocente.TEMPORAL);
        cambio.setFechaFin(hoy.minusDays(1));
        cambio.setDocenteNuevo(sustituto);
        asignacion.setDocente(sustituto);
        CambioDocenteAsignacion detalle = detalle(cambio, titular, sustituto, false);
        when(cambioRepository.findById(7)).thenReturn(Optional.of(cambio));
        when(detalleRepository.findByCambioIdCambioAndAccesoNot(7, eliminado)).thenReturn(List.of(detalle));

        service.finalizarAutomatico(7, hoy);

        assertEquals(EstadoCambioDocente.VIGENTE, cambio.getEstado());
        assertTrue(cambio.getIncidencia());
        assertTrue(cambio.getMotivoIncidencia().contains("ya no está vigente"));
        assertEquals(sustituto, asignacion.getDocente());
        assertFalse(detalle.getDevuelta());
    }

    @Test
    void resolverIncidenciaSoloRegistraObservacionYConservaIncidencia() {
        CambioDocente cambio = cambio(EstadoCambioDocente.VIGENTE, TipoCambioDocente.TEMPORAL);
        cambio.setIncidencia(true);
        cambio.setMotivoIncidencia("Asignación fuera del docente esperado");
        when(cambioRepository.findById(7)).thenReturn(Optional.of(cambio));
        when(detalleRepository.findByCambioIdCambioAndAccesoNot(7, eliminado)).thenReturn(List.of());

        CambioDocenteResponse respuesta = service.resolverIncidencia(
                7, new ResolverIncidenciaRequest("  Observación de dirección  "));

        assertEquals(EstadoCambioDocente.VIGENTE, respuesta.getEstado());
        assertEquals("Observación de dirección", respuesta.getResolucionAdministrativa());
        assertTrue(respuesta.getIncidencia());
        assertEquals("Asignación fuera del docente esperado", respuesta.getMotivoIncidencia());
        assertEquals(hoy, respuesta.getFechaResolucion());
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    void finalizarManualConAsignacionModificadaRegistraIncidenciaSinCambiarElEstado() {
        Docente otro = docente(12, "Otro");
        CambioDocente cambio = cambio(EstadoCambioDocente.VIGENTE, TipoCambioDocente.TEMPORAL);
        cambio.setDocenteNuevo(sustituto);
        asignacion.setDocente(otro);
        CambioDocenteAsignacion detalle = detalle(cambio, titular, sustituto, false);
        when(cambioRepository.findById(7)).thenReturn(Optional.of(cambio));
        when(detalleRepository.findByCambioIdCambioAndAccesoNot(7, eliminado)).thenReturn(List.of(detalle));

        CambioDocenteResponse respuesta = service.finalizarManual(7);

        assertTrue(respuesta.getIncidencia());
        assertNotNull(respuesta.getMotivoIncidencia());
        assertEquals(EstadoCambioDocente.VIGENTE, cambio.getEstado());
        assertFalse(detalle.getDevuelta());
    }

    @Test
    void cancelaSoloCambiosProgramadosSinDevolver() {
        CambioDocente cambio = cambio(EstadoCambioDocente.PROGRAMADO, TipoCambioDocente.TEMPORAL);
        cambio.setDocenteNuevo(sustituto);
        asignacion.setDocente(sustituto);
        CambioDocenteAsignacion detalle = detalle(cambio, titular, sustituto, false);
        when(cambioRepository.bloquear(7)).thenReturn(Optional.of(cambio));
        when(detalleRepository.findByCambioIdCambioAndAccesoNot(7, eliminado)).thenReturn(List.of(detalle));

        CambioDocenteResponse respuesta = service.cancelar(7, "  Solicitud de dirección  ");

        assertEquals(EstadoCambioDocente.CANCELADO, respuesta.getEstado());
        assertEquals("Solicitud de dirección", respuesta.getMotivoResolucion());
        assertEquals(sustituto, asignacion.getDocente());
        assertFalse(detalle.getDevuelta());
        verify(cambioRepository).bloquear(7);
        verify(cambioRepository, never()).findById(7);
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    void noCancelaUnCambioVigente() {
        CambioDocente cambio = cambio(EstadoCambioDocente.VIGENTE, TipoCambioDocente.TEMPORAL);
        when(cambioRepository.bloquear(7)).thenReturn(Optional.of(cambio));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.cancelar(7, "Motivo"));

        assertEquals("Solo se puede cancelar un cambio programado", error.getMessage());
        assertEquals(EstadoCambioDocente.VIGENTE, cambio.getEstado());
        verify(cambioRepository).bloquear(7);
        verify(cambioRepository, never()).findById(7);
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    void asignarSustitutoConAnioCerradoNoModificaLaAsignacion() {
        anio.setEstado(AnioEscolarService.ESTADO_CERRADO);
        CambioDocente cambio = cambio(EstadoCambioDocente.VIGENTE, TipoCambioDocente.TEMPORAL);
        CambioDocenteAsignacion detalle = detalle(cambio, titular, null, true);
        when(cambioRepository.findById(7)).thenReturn(Optional.of(cambio));
        when(detalleRepository.findByCambioIdCambioAndAccesoNot(7, eliminado)).thenReturn(List.of(detalle));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.asignarSustituto(7, new AsignarSustitutoRequest(11)));

        assertTrue(error.getMessage().contains("ya no está vigente"));
        assertEquals(titular, asignacion.getDocente());
        assertTrue(detalle.getSinCobertura());
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    void activarProgramadoConAnioCerradoNoTransfiere() {
        anio.setEstado(AnioEscolarService.ESTADO_CERRADO);
        CambioDocente cambio = cambio(EstadoCambioDocente.PROGRAMADO, TipoCambioDocente.TEMPORAL);
        cambio.setDocenteNuevo(sustituto);
        CambioDocenteAsignacion detalle = detalle(cambio, titular, sustituto, false);
        when(cambioRepository.bloquear(7)).thenReturn(Optional.of(cambio));
        when(detalleRepository.findByCambioIdCambioAndAccesoNot(7, eliminado)).thenReturn(List.of(detalle));

        assertThrows(IllegalArgumentException.class, () -> service.activarProgramado(7, hoy));

        assertEquals(EstadoCambioDocente.PROGRAMADO, cambio.getEstado());
        assertEquals(titular, asignacion.getDocente());
        verify(cambioRepository).bloquear(7);
        verify(cambioRepository, never()).findById(7);
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    void finalizarManualConIncidenciaLimpiaBanderaTrasDevolver() {
        CambioDocente cambio = cambio(EstadoCambioDocente.VIGENTE, TipoCambioDocente.TEMPORAL);
        cambio.setDocenteNuevo(sustituto);
        cambio.setIncidencia(true);
        cambio.setMotivoIncidencia("Asignación fuera del docente esperado");
        asignacion.setDocente(sustituto);
        CambioDocenteAsignacion detalle = detalle(cambio, titular, sustituto, false);
        when(cambioRepository.findById(7)).thenReturn(Optional.of(cambio));
        when(detalleRepository.findByCambioIdCambioAndAccesoNot(7, eliminado)).thenReturn(List.of(detalle));

        service.finalizarManual(7);

        assertEquals(EstadoCambioDocente.FINALIZADO, cambio.getEstado());
        assertFalse(cambio.getIncidencia());
        assertNull(cambio.getMotivoIncidencia());
        assertEquals(titular, asignacion.getDocente());
        assertTrue(detalle.getDevuelta());
    }

    @Test
    void rechazaSustitutoConHorarioCruzado() {
        Asignacion otra = asignacion(101, sustituto);
        HorarioClase clase = horario(asignacion, (byte) 1, LocalTime.of(8, 0), LocalTime.of(9, 0));
        HorarioClase otraClase = horario(otra, (byte) 1, LocalTime.of(8, 30), LocalTime.of(9, 30));
        when(horarioClaseRepository.findByAsignacionIdAsignacionInAndAccesoNot(List.of(100), eliminado))
                .thenReturn(List.of(clase));
        when(horarioClaseRepository
                .findByAsignacionDocenteIdDocenteAndAsignacionAnioEscolarIdAnioAndAccesoNot(11, 5, eliminado))
                .thenReturn(List.of(otraClase));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(new CambioDocenteRequest(
                        TipoCambioDocente.TEMPORAL, hoy, hoy.plusDays(7), 1, "Licencia", List.of(100), 11)));

        assertTrue(ex.getMessage().contains("ya tiene clase el lunes de 08:00–09:00"));
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    void activarProgramadoRechazaCruceDeHorarios() {
        CambioDocente cambio = cambio(EstadoCambioDocente.PROGRAMADO, TipoCambioDocente.TEMPORAL);
        cambio.setDocenteNuevo(sustituto);
        CambioDocenteAsignacion detalle = detalle(cambio, titular, sustituto, false);
        when(cambioRepository.bloquear(7)).thenReturn(Optional.of(cambio));
        when(detalleRepository.findByCambioIdCambioAndAccesoNot(7, eliminado)).thenReturn(List.of(detalle));
        Asignacion otra = asignacion(101, sustituto);
        when(horarioClaseRepository.findByAsignacionIdAsignacionInAndAccesoNot(List.of(100), eliminado))
                .thenReturn(List.of(horario(asignacion, (byte) 1, LocalTime.of(8, 0), LocalTime.of(9, 0))));
        when(horarioClaseRepository
                .findByAsignacionDocenteIdDocenteAndAsignacionAnioEscolarIdAnioAndAccesoNot(11, 5, eliminado))
                .thenReturn(List.of(horario(otra, (byte) 1, LocalTime.of(8, 30), LocalTime.of(9, 30))));

        assertThrows(IllegalArgumentException.class, () -> service.activarProgramado(7, hoy));

        assertEquals(EstadoCambioDocente.PROGRAMADO, cambio.getEstado());
        verify(cambioRepository).bloquear(7);
        verify(cambioRepository, never()).findById(7);
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    void activarProgramadoBloqueaCambioAntesDeAsignaciones() {
        CambioDocente cambio = cambio(EstadoCambioDocente.PROGRAMADO, TipoCambioDocente.TEMPORAL);
        cambio.setDocenteNuevo(sustituto);
        CambioDocenteAsignacion detalle = detalle(cambio, titular, sustituto, false);
        when(cambioRepository.bloquear(7)).thenReturn(Optional.of(cambio));
        when(detalleRepository.findByCambioIdCambioAndAccesoNot(7, eliminado)).thenReturn(List.of(detalle));
        when(horarioClaseRepository.findByAsignacionIdAsignacionInAndAccesoNot(List.of(100), eliminado))
                .thenReturn(List.of());
        when(horarioClaseRepository
                .findByAsignacionDocenteIdDocenteAndAsignacionAnioEscolarIdAnioAndAccesoNot(11, 5, eliminado))
                .thenReturn(List.of());

        service.activarProgramado(7, hoy);

        assertEquals(EstadoCambioDocente.VIGENTE, cambio.getEstado());
        assertEquals(sustituto, asignacion.getDocente());
        InOrder orden = inOrder(cambioRepository, bloqueo);
        orden.verify(cambioRepository).bloquear(7);
        orden.verify(bloqueo).asignacion(100);
    }

    @Test
    void finalizarAutomaticoRegistraIncidenciaPorCruceAlDevolver() {
        CambioDocente cambio = cambio(EstadoCambioDocente.VIGENTE, TipoCambioDocente.TEMPORAL);
        cambio.setFechaFin(hoy.minusDays(1));
        cambio.setDocenteNuevo(sustituto);
        asignacion.setDocente(sustituto);
        CambioDocenteAsignacion detalle = detalle(cambio, titular, sustituto, false);
        when(cambioRepository.findById(7)).thenReturn(Optional.of(cambio));
        when(detalleRepository.findByCambioIdCambioAndAccesoNot(7, eliminado)).thenReturn(List.of(detalle));
        Asignacion otra = asignacion(102, titular);
        when(horarioClaseRepository.findByAsignacionIdAsignacionInAndAccesoNot(List.of(100), eliminado))
                .thenReturn(List.of(horario(asignacion, (byte) 1, LocalTime.of(8, 0), LocalTime.of(9, 0))));
        when(horarioClaseRepository
                .findByAsignacionDocenteIdDocenteAndAsignacionAnioEscolarIdAnioAndAccesoNot(10, 5, eliminado))
                .thenReturn(List.of(horario(otra, (byte) 1, LocalTime.of(8, 30), LocalTime.of(9, 30))));

        service.finalizarAutomatico(7, hoy);

        assertTrue(cambio.getIncidencia());
        assertTrue(cambio.getMotivoIncidencia().contains("docente receptor"));
        assertEquals(sustituto, asignacion.getDocente());
        assertFalse(detalle.getDevuelta());
    }

    @Test
    void rechazaAsignacionesSeleccionadasConHorarioCruzado() {
        Asignacion otra = asignacion(101, titular);
        when(asignacionRepository.findAllById(List.of(100, 101))).thenReturn(List.of(asignacion, otra));
        when(horarioClaseRepository.findByAsignacionIdAsignacionInAndAccesoNot(List.of(100, 101), eliminado))
                .thenReturn(List.of(
                        horario(asignacion, (byte) 1, LocalTime.of(8, 0), LocalTime.of(9, 0)),
                        horario(otra, (byte) 1, LocalTime.of(8, 30), LocalTime.of(9, 30))));
        when(horarioClaseRepository
                .findByAsignacionDocenteIdDocenteAndAsignacionAnioEscolarIdAnioAndAccesoNot(11, 5, eliminado))
                .thenReturn(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(new CambioDocenteRequest(
                        TipoCambioDocente.TEMPORAL, hoy, null, 1, "Licencia", List.of(100, 101), 11)));

        assertTrue(ex.getMessage().contains("asignaciones seleccionadas tienen un cruce"));
        verify(detalleRepository, never()).save(any());
    }

    @Test
    void activarProgramadoRechazaAsignacionFueraDelDocenteEsperado() {
        CambioDocente cambio = cambio(EstadoCambioDocente.PROGRAMADO, TipoCambioDocente.TEMPORAL);
        cambio.setDocenteNuevo(sustituto);
        asignacion.setDocente(sustituto);
        CambioDocenteAsignacion detalle = detalle(cambio, titular, sustituto, false);
        when(cambioRepository.bloquear(7)).thenReturn(Optional.of(cambio));
        when(detalleRepository.findByCambioIdCambioAndAccesoNot(7, eliminado)).thenReturn(List.of(detalle));
        when(horarioClaseRepository.findByAsignacionIdAsignacionInAndAccesoNot(List.of(100), eliminado))
                .thenReturn(List.of());
        when(horarioClaseRepository
                .findByAsignacionDocenteIdDocenteAndAsignacionAnioEscolarIdAnioAndAccesoNot(11, 5, eliminado))
                .thenReturn(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.activarProgramado(7, hoy));

        assertTrue(ex.getMessage().contains("no está en el docente esperado"));
        assertEquals(EstadoCambioDocente.PROGRAMADO, cambio.getEstado());
        assertEquals(sustituto, asignacion.getDocente());
        verify(cambioRepository).bloquear(7);
        verify(cambioRepository, never()).findById(7);
    }

    @Test
    void finalizarAutomaticoNoDevuelveParcialmenteCuandoFallaUnaValidacion() {
        CambioDocente cambio = cambio(EstadoCambioDocente.VIGENTE, TipoCambioDocente.TEMPORAL);
        cambio.setFechaFin(hoy.minusDays(1));
        cambio.setDocenteNuevo(sustituto);
        Asignacion primera = asignacion(100, sustituto);
        Asignacion segunda = asignacion(101, titular);
        CambioDocenteAsignacion detalleUno = detalle(cambio, titular, sustituto, false);
        detalleUno.setAsignacion(primera);
        CambioDocenteAsignacion detalleDos = detalle(cambio, titular, sustituto, false);
        detalleDos.setAsignacion(segunda);
        when(cambioRepository.findById(7)).thenReturn(Optional.of(cambio));
        when(detalleRepository.findByCambioIdCambioAndAccesoNot(7, eliminado))
                .thenReturn(List.of(detalleUno, detalleDos));
        when(horarioClaseRepository.findByAsignacionIdAsignacionInAndAccesoNot(List.of(100, 101), eliminado))
                .thenReturn(List.of());
        when(horarioClaseRepository
                .findByAsignacionDocenteIdDocenteAndAsignacionAnioEscolarIdAnioAndAccesoNot(10, 5, eliminado))
                .thenReturn(List.of());

        service.finalizarAutomatico(7, hoy);

        assertTrue(cambio.getIncidencia());
        assertEquals(sustituto, primera.getDocente());
        assertEquals(titular, segunda.getDocente());
        assertFalse(detalleUno.getDevuelta());
        assertFalse(detalleDos.getDevuelta());
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    void editaProgramadoReutilizaDetallesYBajaLogicoLosQueSeRetiran() {
        Asignacion segunda = asignacion(101, titular);
        Asignacion retirada = asignacion(102, titular);
        when(asignacionRepository.findAllById(List.of(100, 101))).thenReturn(List.of(asignacion, segunda));
        when(motivoRepository.findByIdMotivoCambioDocenteAndAcceso(2, activo)).thenReturn(Optional.of(motivo));
        CambioDocente cambio = cambio(EstadoCambioDocente.PROGRAMADO, TipoCambioDocente.TEMPORAL);
        cambio.setFechaInicio(hoy.plusDays(2));
        cambio.setFechaFin(hoy.plusDays(6));
        cambio.setDocenteNuevo(sustituto);
        cambio.setMotivoDetalle("Motivo original");
        CambioDocenteAsignacion detalleUno = detalle(cambio, asignacion, titular, sustituto, false);
        CambioDocenteAsignacion detalleRetirado = detalle(cambio, retirada, titular, sustituto, false);
        detallesGuardados.add(detalleUno);
        detallesGuardados.add(detalleRetirado);
        when(cambioRepository.findById(7)).thenReturn(Optional.of(cambio));

        CambioDocenteResponse respuesta = service.actualizar(7, new CambioDocenteRequest(
                TipoCambioDocente.TEMPORAL, hoy.plusDays(3), hoy.plusDays(8), 2,
                "Detalle editado", List.of(100, 101), 11));

        assertEquals(EstadoCambioDocente.PROGRAMADO, respuesta.getEstado());
        assertEquals(hoy.plusDays(3), cambio.getFechaInicio());
        assertEquals("Detalle editado", cambio.getMotivoDetalle());
        assertEquals(OrigenOperacion.MANUAL, cambio.getOrigenUltimaOperacion());
        assertEquals(usuario, cambio.getUsuarioUltimaOperacion());
        assertEquals(usuario, cambio.getUsuarioRegistro());
        assertEquals(OrigenOperacion.MANUAL, cambio.getOrigenRegistro());
        assertEquals(eliminado, detalleRetirado.getAcceso());
        assertFalse(detalleUno.getDevuelta());
        assertEquals(2, respuesta.getDetalles().size());

        ArgumentCaptor<CambioDocenteAsignacion> captor = ArgumentCaptor.forClass(CambioDocenteAsignacion.class);
        verify(detalleRepository, times(3)).save(captor.capture());
        assertEquals(1, captor.getAllValues().stream().filter(d -> d == detalleUno).count());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(d -> d.getAsignacion().getIdAsignacion().equals(101)));
    }

    @Test
    void editaProgramadoPuedeCambiarTipoFechaFinTitularYSustituto() {
        Docente otroTitular = docente(12, "Otro titular");
        Docente otroSustituto = docente(13, "Otro sustituto");
        Asignacion nuevaAsignacion = asignacion(103, otroTitular);
        when(asignacionRepository.findAllById(List.of(103))).thenReturn(List.of(nuevaAsignacion));
        when(docenteRepository.findById(13)).thenReturn(Optional.of(otroSustituto));
        CambioDocente cambio = cambio(EstadoCambioDocente.PROGRAMADO, TipoCambioDocente.TEMPORAL);
        cambio.setFechaInicio(hoy.plusDays(2));
        cambio.setDocenteNuevo(sustituto);
        CambioDocenteAsignacion anterior = detalle(cambio, asignacion, titular, sustituto, false);
        detallesGuardados.add(anterior);
        when(cambioRepository.findById(7)).thenReturn(Optional.of(cambio));

        CambioDocenteResponse respuesta = service.actualizar(7, new CambioDocenteRequest(
                TipoCambioDocente.DEFINITIVO, hoy.plusDays(2), null, 1,
                "Traslado", List.of(103), 13));

        assertEquals(TipoCambioDocente.DEFINITIVO, respuesta.getTipo());
        assertEquals(EstadoCambioDocente.PROGRAMADO, respuesta.getEstado());
        assertNull(cambio.getFechaFin());
        assertEquals(otroTitular, cambio.getDocenteAnterior());
        assertEquals(otroSustituto, cambio.getDocenteNuevo());
        assertEquals(eliminado, anterior.getAcceso());
        assertEquals(1, respuesta.getDetalles().size());
    }

    @Test
    void editaVigenteSoloMotivoFechaFinYObservacionSinDuplicarTransferencia() {
        CambioDocente cambio = cambio(EstadoCambioDocente.VIGENTE, TipoCambioDocente.TEMPORAL);
        cambio.setFechaFin(hoy.plusDays(2));
        cambio.setDocenteNuevo(sustituto);
        cambio.setMotivoDetalle("Motivo original");
        cambio.setIncidencia(true);
        cambio.setMotivoIncidencia("Asignación revisada");
        asignacion.setDocente(sustituto);
        CambioDocenteAsignacion detalle = detalle(cambio, asignacion, titular, sustituto, false);
        detalle.setFechaTransferencia(hoy.minusDays(3));
        detallesGuardados.add(detalle);
        when(cambioRepository.findById(7)).thenReturn(Optional.of(cambio));

        CambioDocenteResponse respuesta = service.editarVigente(7, new EditarCambioVigenteRequest(1, "Observaci\u00f3n editada", hoy.plusDays(10)));

        assertEquals(EstadoCambioDocente.VIGENTE, respuesta.getEstado());
        assertEquals(hoy.plusDays(10), cambio.getFechaFin());
        assertEquals("Observación editada", cambio.getMotivoDetalle());
        assertEquals(titular, cambio.getDocenteAnterior());
        assertEquals(sustituto, cambio.getDocenteNuevo());
        assertTrue(cambio.getIncidencia());
        assertNotNull(cambio.getMotivoIncidencia());
        assertEquals(hoy.minusDays(3), detalle.getFechaTransferencia());
        assertEquals(sustituto, asignacion.getDocente());
        assertEquals(OrigenOperacion.MANUAL, cambio.getOrigenUltimaOperacion());
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    void editaVigenteRechazaTipoFechaInicioAsignacionesYSustitutoDistintos() {
        CambioDocente cambio = cambio(EstadoCambioDocente.VIGENTE, TipoCambioDocente.TEMPORAL);
        cambio.setDocenteNuevo(sustituto);
        asignacion.setDocente(sustituto);
        detallesGuardados.add(detalle(cambio, asignacion, titular, sustituto, false));
        when(cambioRepository.findById(7)).thenReturn(Optional.of(cambio));

        List<CambioDocenteRequest> intentos = List.of(
                new CambioDocenteRequest(TipoCambioDocente.DEFINITIVO, hoy.minusDays(3), null, 1,
                        "Obs", List.of(100), 11),
                new CambioDocenteRequest(TipoCambioDocente.TEMPORAL, hoy.minusDays(2), hoy.plusDays(7), 1,
                        "Obs", List.of(100), 11),
                new CambioDocenteRequest(TipoCambioDocente.TEMPORAL, hoy.minusDays(3), hoy.plusDays(7), 1,
                        "Obs", List.of(101), 11),
                new CambioDocenteRequest(TipoCambioDocente.TEMPORAL, hoy.minusDays(3), hoy.plusDays(7), 1,
                        "Obs", List.of(100), 12));
        for (int i = 0; i < intentos.size(); i++) {
            final int indice = i;
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> service.actualizar(7, intentos.get(indice)));
            assertTrue(error.getMessage().contains("cambios programados"), error.getMessage());
        }
        assertEquals(TipoCambioDocente.TEMPORAL, cambio.getTipo());
        assertEquals(hoy.minusDays(3), cambio.getFechaInicio());
        assertEquals(sustituto, cambio.getDocenteNuevo());
        verify(cambioRepository, never()).save(any());
        verify(detalleRepository, never()).save(any());
    }

    @Test
    void putConvencionalRechazaUnCambioVigenteInclusoConDetallesInconsistentes() {
        CambioDocente cambio = cambio(EstadoCambioDocente.VIGENTE, TipoCambioDocente.TEMPORAL);
        cambio.setDocenteAnterior(docente(12, "Otro titular"));
        cambio.setDocenteNuevo(sustituto);
        detallesGuardados.add(detalle(cambio, asignacion, titular, sustituto, false));
        when(cambioRepository.findById(7)).thenReturn(Optional.of(cambio));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.actualizar(7, new CambioDocenteRequest(
                        TipoCambioDocente.TEMPORAL, hoy.minusDays(3), hoy.plusDays(7), 1,
                        "Obs", List.of(100), 11)));

        assertTrue(error.getMessage().contains("cambios programados"));
    }

    @Test
    void editaRechazaCambiosAplicadosFinalizadosYCancelados() {
        for (EstadoCambioDocente estado : List.of(EstadoCambioDocente.APLICADO,
                EstadoCambioDocente.FINALIZADO, EstadoCambioDocente.CANCELADO)) {
            CambioDocente cambio = cambio(estado, TipoCambioDocente.TEMPORAL);
            when(cambioRepository.findById(7)).thenReturn(Optional.of(cambio));

            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> service.actualizar(7, new CambioDocenteRequest(
                            TipoCambioDocente.TEMPORAL, hoy, hoy.plusDays(7), 1,
                            "Obs", List.of(100), 11)));

            assertTrue(error.getMessage().contains("cambios programados"));
            assertEquals(estado, cambio.getEstado());
        }
        verify(cambioRepository, never()).save(any());
    }

    @Test
    void editaProgramadoNoSeConsideraIncompatibleConSusPropiosDetalles() {
        CambioDocente cambio = cambio(EstadoCambioDocente.PROGRAMADO, TipoCambioDocente.TEMPORAL);
        cambio.setFechaInicio(hoy.plusDays(2));
        cambio.setDocenteNuevo(sustituto);
        CambioDocenteAsignacion detalle = detalle(cambio, asignacion, titular, sustituto, false);
        detallesGuardados.add(detalle);
        when(cambioRepository.findById(7)).thenReturn(Optional.of(cambio));
        when(detalleRepository.findByAsignacionIdAsignacionInAndDevueltaFalseAndAccesoNot(List.of(100), eliminado))
                .thenReturn(List.of(detalle));

        CambioDocenteResponse respuesta = service.actualizar(7, new CambioDocenteRequest(
                TipoCambioDocente.TEMPORAL, hoy.plusDays(2), hoy.plusDays(7), 1,
                "Obs", List.of(100), 11));

        assertEquals(EstadoCambioDocente.PROGRAMADO, respuesta.getEstado());
    }

    @Test
    void editaProgramadoRechazaAsignacionEnOtroCambioActivo() {
        CambioDocente cambio = cambio(EstadoCambioDocente.PROGRAMADO, TipoCambioDocente.TEMPORAL);
        cambio.setFechaInicio(hoy.plusDays(2));
        cambio.setDocenteNuevo(sustituto);
        detallesGuardados.add(detalle(cambio, asignacion, titular, sustituto, false));
        when(cambioRepository.findById(7)).thenReturn(Optional.of(cambio));
        CambioDocente otro = cambio(EstadoCambioDocente.VIGENTE, TipoCambioDocente.TEMPORAL);
        otro.setIdCambio(8);
        CambioDocenteAsignacion detalleAjeno = detalle(otro, asignacion, titular, sustituto, false);
        when(detalleRepository.findByAsignacionIdAsignacionInAndDevueltaFalseAndAccesoNot(List.of(100), eliminado))
                .thenReturn(List.of(detalleAjeno));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.actualizar(7, new CambioDocenteRequest(
                        TipoCambioDocente.TEMPORAL, hoy.plusDays(2), hoy.plusDays(7), 1,
                        "Obs", List.of(100), 11)));

        assertTrue(error.getMessage().contains("ya participa en un cambio de docente pendiente"));
        assertEquals(EstadoCambioDocente.PROGRAMADO, cambio.getEstado());
        verify(cambioRepository, never()).save(any());
    }

    @Test
    void editaProgramadoConFechaInicioHoyActivaSinTransferirDosVeces() {
        Asignacion yaTransferida = asignacion(101, titular);
        when(asignacionRepository.findAllById(List.of(100, 101))).thenReturn(List.of(asignacion, yaTransferida));
        CambioDocente cambio = cambio(EstadoCambioDocente.PROGRAMADO, TipoCambioDocente.TEMPORAL);
        cambio.setFechaInicio(hoy.plusDays(1));
        cambio.setDocenteNuevo(sustituto);
        CambioDocenteAsignacion detalleTransferido = detalle(cambio, yaTransferida, titular, sustituto, false);
        detalleTransferido.setFechaTransferencia(hoy.minusDays(1));
        detallesGuardados.add(detalleTransferido);
        when(cambioRepository.findById(7)).thenReturn(Optional.of(cambio));

        CambioDocenteResponse respuesta = service.actualizar(7, new CambioDocenteRequest(
                TipoCambioDocente.TEMPORAL, hoy, hoy.plusDays(7), 1,
                "Obs", List.of(100, 101), 11));

        assertEquals(EstadoCambioDocente.VIGENTE, respuesta.getEstado());
        assertEquals(sustituto, asignacion.getDocente());
        assertEquals(titular, yaTransferida.getDocente());
        assertEquals(hoy.minusDays(1), detalleTransferido.getFechaTransferencia());
        CambioDocenteAsignacion detalleNuevo = detallesGuardados.stream()
                .filter(d -> d.getAsignacion().getIdAsignacion().equals(100))
                .findFirst().orElseThrow();
        assertEquals(hoy, detalleNuevo.getFechaTransferencia());
        verify(asignacionRepository, times(1)).save(any());
    }

    @Test
    void finalizarManualConIncidenciaQueFallaNoLimpiaLaAuditoria() {
        Docente otro = docente(12, "Otro");
        CambioDocente cambio = cambio(EstadoCambioDocente.VIGENTE, TipoCambioDocente.TEMPORAL);
        cambio.setDocenteNuevo(sustituto);
        cambio.setIncidencia(true);
        cambio.setMotivoIncidencia("Asignación revisada");
        cambio.setResolucionAdministrativa("Observación previa");
        asignacion.setDocente(otro);
        detallesGuardados.add(detalle(cambio, asignacion, titular, sustituto, false));
        when(cambioRepository.findById(7)).thenReturn(Optional.of(cambio));

        service.finalizarManual(7);

        assertTrue(cambio.getIncidencia());
        assertEquals("Asignación revisada", cambio.getMotivoIncidencia());
        assertEquals("Observación previa", cambio.getResolucionAdministrativa());
        assertEquals(EstadoCambioDocente.VIGENTE, cambio.getEstado());
        verify(cambioRepository).save(cambio);
    }

    @Test
    void respuestaIncluyeDatosAcademicosActualesDeLaAsignacion() {
        Curso curso = new Curso();
        curso.setNombre("Matemática");
        Grado grado = new Grado();
        grado.setNombre("Tercero");
        Seccion seccion = new Seccion();
        seccion.setNombre("A");
        GradoSeccion gradoSeccion = new GradoSeccion();
        gradoSeccion.setGrado(grado);
        gradoSeccion.setSeccion(seccion);
        asignacion.setCurso(curso);
        asignacion.setGradoSeccion(gradoSeccion);
        CambioDocente cambio = cambio(EstadoCambioDocente.VIGENTE, TipoCambioDocente.TEMPORAL);
        cambio.setDocenteNuevo(sustituto);
        detallesGuardados.add(detalle(cambio, asignacion, titular, sustituto, false));
        when(cambioRepository.findById(7)).thenReturn(Optional.of(cambio));

        CambioDocenteResponse.Detalle detalle = service.getById(7).getDetalles().get(0);

        assertEquals("Matemática", detalle.curso());
        assertEquals("Tercero", detalle.grado());
        assertEquals("A", detalle.seccion());
        assertEquals("2026", detalle.anio());
    }

    @Test
    void detalleIncluyeHorariosEnUnaSolaConsulta() {
        CambioDocente cambio = cambio(EstadoCambioDocente.VIGENTE, TipoCambioDocente.TEMPORAL);
        cambio.setDocenteNuevo(sustituto);
        detallesGuardados.add(detalle(cambio, asignacion, titular, sustituto, false));
        HorarioClase temprano = horario(asignacion, (byte) 1, LocalTime.of(7, 0), LocalTime.of(8, 0));
        temprano.setIdHorario(54);
        HorarioClase tarde = horario(asignacion, (byte) 3, LocalTime.of(10, 0), LocalTime.of(11, 0));
        tarde.setIdHorario(55);
        when(horarioClaseRepository.findByAsignacionIdAsignacionInAndAccesoNot(List.of(100), eliminado))
                .thenReturn(List.of(tarde, temprano));
        when(cambioRepository.findById(7)).thenReturn(Optional.of(cambio));

        CambioDocenteResponse.Detalle detalle = service.getById(7).getDetalles().get(0);

        assertEquals(2, detalle.horarios().size());
        assertEquals(54, detalle.horarios().get(0).idHorario());
        assertEquals(1, detalle.horarios().get(0).diaSemana());
        assertEquals("07:00", detalle.horarios().get(0).horaInicio());
        assertEquals("08:00", detalle.horarios().get(0).horaFin());
        assertEquals(55, detalle.horarios().get(1).idHorario());
        verify(horarioClaseRepository, times(1))
                .findByAsignacionIdAsignacionInAndAccesoNot(eq(List.of(100)), eq(eliminado));
        verify(horarioClaseRepository, never()).findByAsignacionIdAsignacionAndAccesoNot(anyInt(), any());
    }

    @Test
    void getAllCargaDetallesYHorariosEnBloque() {
        CambioDocente primero = cambio(EstadoCambioDocente.VIGENTE, TipoCambioDocente.TEMPORAL);
        CambioDocente segundo = cambio(EstadoCambioDocente.VIGENTE, TipoCambioDocente.TEMPORAL);
        segundo.setIdCambio(8);
        Asignacion otra = asignacion(200, titular);
        List<CambioDocenteAsignacion> detalles = List.of(
                detalle(primero, asignacion, titular, sustituto, false),
                detalle(segundo, otra, titular, sustituto, false));
        when(cambioRepository.findByAccesoNot(eliminado)).thenReturn(List.of(primero, segundo));
        when(detalleRepository.findByCambioIdCambioInAndAccesoNot(List.of(7, 8), eliminado))
                .thenReturn(detalles);
        when(horarioClaseRepository.findByAsignacionIdAsignacionInAndAccesoNot(List.of(100, 200), eliminado))
                .thenReturn(List.of());

        List<CambioDocenteResponse> respuesta = service.getAll();

        assertEquals(2, respuesta.size());
        assertEquals(1, respuesta.get(0).getDetalles().size());
        assertEquals(1, respuesta.get(1).getDetalles().size());
        verify(detalleRepository, times(1))
                .findByCambioIdCambioInAndAccesoNot(eq(List.of(7, 8)), eq(eliminado));
        verify(detalleRepository, never()).findByCambioIdCambioAndAccesoNot(anyInt(), any());
        verify(horarioClaseRepository, times(1))
                .findByAsignacionIdAsignacionInAndAccesoNot(eq(List.of(100, 200)), eq(eliminado));
    }

    private Asignacion asignacion(int id, Docente docente) {
        Asignacion asignacion = new Asignacion();
        asignacion.setIdAsignacion(id);
        asignacion.setDocente(docente);
        asignacion.setAnioEscolar(anio);
        asignacion.setAcceso(activo);
        return asignacion;
    }

    private HorarioClase horario(Asignacion asignacion, byte dia, LocalTime inicio, LocalTime fin) {
        HorarioClase horario = new HorarioClase();
        horario.setAsignacion(asignacion);
        horario.setDiaSemana(dia);
        horario.setHoraInicio(inicio);
        horario.setHoraFin(fin);
        horario.setAcceso(activo);
        return horario;
    }

    private Acceso acceso(long id) {
        Acceso acceso = new Acceso();
        acceso.setIdAcceso(id);
        return acceso;
    }

    private Docente docente(int id, String nombre) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setApellidoPat("Apellido");
        u.setApellidoMat("Materno");
        Docente docente = new Docente();
        docente.setIdDocente(id);
        docente.setAcceso(activo);
        docente.setUsuario(u);
        return docente;
    }

    private CambioDocente cambio(EstadoCambioDocente estado, TipoCambioDocente tipo) {
        CambioDocente cambio = new CambioDocente();
        cambio.setIdCambio(7);
        cambio.setEstado(estado);
        cambio.setTipo(tipo);
        cambio.setAnioEscolar(anio);
        cambio.setDocenteAnterior(titular);
        cambio.setAcceso(activo);
        cambio.setIncidencia(false);
        cambio.setFechaInicio(hoy.minusDays(3));
        cambio.setMotivo("Licencia");
        cambio.setFechaCambio(hoy);
        cambio.setUsuarioRegistro(usuario);
        cambio.setOrigenRegistro(OrigenOperacion.MANUAL);
        cambio.setOrigenUltimaOperacion(OrigenOperacion.MANUAL);
        return cambio;
    }

    private CambioDocenteAsignacion detalle(CambioDocente cambio, Docente desde, Docente hacia,
                                             boolean sinCobertura) {
        return detalle(cambio, asignacion, desde, hacia, sinCobertura);
    }

    private CambioDocenteAsignacion detalle(CambioDocente cambio, Asignacion asignacion, Docente desde,
                                             Docente hacia, boolean sinCobertura) {
        CambioDocenteAsignacion detalle = new CambioDocenteAsignacion();
        detalle.setCambio(cambio);
        detalle.setAsignacion(asignacion);
        detalle.setDocenteDesde(desde);
        detalle.setDocenteHacia(hacia);
        detalle.setSinCobertura(sinCobertura);
        detalle.setDevuelta(false);
        detalle.setAcceso(activo);
        return detalle;
    }
}
