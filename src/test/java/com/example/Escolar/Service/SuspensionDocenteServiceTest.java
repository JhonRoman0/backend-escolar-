package com.example.Escolar.Service;

import com.example.Escolar.Dto.SuspensionDocenteRequest;
import com.example.Escolar.Dto.EditarSuspensionVigenteRequest;
import com.example.Escolar.Dto.SuspensionDocenteResponse;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Asignacion;
import com.example.Escolar.Model.CambioDocente;
import com.example.Escolar.Model.CambioDocenteAsignacion;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.EstadoCambioDocente;
import com.example.Escolar.Model.EstadoSuspension;
import com.example.Escolar.Model.SuspensionDocente;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.CambioDocenteAsignacionRepository;
import com.example.Escolar.Repository.DocenteRepository;
import com.example.Escolar.Repository.SuspensionDocenteRepository;
import com.example.Escolar.Security.ConcesionesService;
import com.example.Escolar.Util.FechaColegio;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SuspensionDocenteServiceTest {

    @Mock
    private SuspensionDocenteRepository suspensionDocenteRepository;
    @Mock
    private DocenteRepository docenteRepository;
    @Mock
    private CambioDocenteAsignacionRepository detalleRepository;
    @Mock
    private AsignacionRepository asignacionRepository;
    @Mock
    private AccesoRepository accesoRepository;
    @Mock
    private AnioEscolarRepository anioEscolarRepository;
    @Mock
    private ConcesionesService concesionesService;
    @Mock
    private BloqueoCalificaciones bloqueoCalificaciones;
    @Mock
    private CambioDocenteService cambioDocenteService;
    @InjectMocks
    private SuspensionDocenteService service;

    private Acceso activo;
    private Acceso inactivo;
    private Acceso eliminado;
    private Docente titular;
    private Docente sustituto;
    private LocalDate hoy;

    @BeforeEach
    void setUp() {
        hoy = FechaColegio.hoy();
        activo = acceso((long) AccesoConstants.ACTIVO);
        inactivo = acceso((long) AccesoConstants.INACTIVO);
        eliminado = acceso((long) AccesoConstants.ELIMINADO);
        titular = docente(10, "Titular");
        sustituto = docente(11, "Sustituto");
        when(cambioDocenteService.cerrarCobertura(any(), any(), anyBoolean())).thenReturn(true);
        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(activo));
        when(accesoRepository.findById(AccesoConstants.INACTIVO)).thenReturn(Optional.of(inactivo));
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        when(docenteRepository.findById(10)).thenReturn(Optional.of(titular));
        when(docenteRepository.findById(11)).thenReturn(Optional.of(sustituto));
        when(suspensionDocenteRepository.existsByDocenteAndAcceso(titular, activo)).thenReturn(false);
        when(suspensionDocenteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(docenteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(asignacionRepository.findByDocenteIdDocenteAndAccesoNot(anyInt(), eq(eliminado)))
                .thenReturn(List.of());
        when(detalleRepository.findByAsignacionIdAsignacionInAndDevueltaFalseAndAccesoNot(any(), eq(eliminado)))
                .thenReturn(List.of());
        when(detalleRepository.findByDocenteHaciaIdDocenteAndDevueltaFalseAndAccesoNot(anyInt(), eq(eliminado)))
                .thenReturn(List.of());
        when(suspensionDocenteRepository.existsByDocenteAndEstadoInAndAccesoNot(any(), any(), eq(eliminado)))
                .thenReturn(false);
        when(suspensionDocenteRepository.existsByDocenteIdDocenteAndEstadoInAndAccesoNotAndIdSuspensionNot(
                anyInt(), any(), eq(eliminado), any())).thenReturn(false);
    }

    @Test
    void creaSuspensionEInactivaAlDocenteSinTocarAsignaciones() {
        SuspensionDocenteResponse respuesta = service.create(request(10, null));

        assertEquals(inactivo, titular.getAcceso());
        verify(docenteRepository).save(titular);
        assertEquals(AccesoConstants.ACTIVO, sustituto.getAcceso().getIdAcceso());
        assertEquals(EstadoSuspension.VIGENTE.name(), respuesta.getEstado());
        verify(bloqueoCalificaciones).docente(10);
    }

    @Test
    void creaRechazaSustitutoConSuspensionProgramadaOVigente() {
        when(suspensionDocenteRepository.existsByDocenteAndEstadoInAndAccesoNot(
                eq(sustituto), eq(List.of(EstadoSuspension.PROGRAMADA, EstadoSuspension.VIGENTE)), eq(eliminado)))
                .thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.create(request(10, 11)));

        assertTrue(error.getMessage().contains("suspensión activa"));
        assertEquals(activo, sustituto.getAcceso());
        verify(docenteRepository, never()).save(any());
    }

    @Test
    void finalizarBloqueaSiElSustitutoTieneCambiosPendientes() {
        SuspensionDocente suspension = suspension(titular, sustituto);
        when(suspensionDocenteRepository.findByIdSuspensionAndAccesoNot(5, eliminado))
                .thenReturn(Optional.of(suspension));
        CambioDocente cambio = new CambioDocente();
        cambio.setEstado(EstadoCambioDocente.VIGENTE);
        CambioDocenteAsignacion detalle = new CambioDocenteAsignacion();
        detalle.setCambio(cambio);
        when(detalleRepository.findByDocenteHaciaIdDocenteAndDevueltaFalseAndAccesoNot(11, eliminado))
                .thenReturn(List.of(detalle));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.finalizar(5));

        assertTrue(error.getMessage().contains("Cambios de docente"));
        assertEquals(inactivo, titular.getAcceso());
    }

    @Test
    void finalizarReactivaAlDocenteCuandoNoHayCambiosPendientes() {
        SuspensionDocente suspension = suspension(titular, sustituto);
        when(suspensionDocenteRepository.findByIdSuspensionAndAccesoNot(5, eliminado))
                .thenReturn(Optional.of(suspension));
        when(detalleRepository.findByDocenteHaciaIdDocenteAndDevueltaFalseAndAccesoNot(11, eliminado))
                .thenReturn(List.of());

        service.finalizar(5);

        assertEquals(activo, titular.getAcceso());
        assertEquals(EstadoSuspension.FINALIZADA, suspension.getEstado());
        assertEquals(activo, suspension.getAcceso());
        assertEquals(hoy, suspension.getFechaFinalizacion());
        verify(docenteRepository).save(titular);
    }

    @Test
    void actualizaSuspensionActivaSoloMotivoDetalleYFechaFin() {
        SuspensionDocente suspension = suspension(titular, sustituto);
        suspension.setMotivoDetalle("Detalle anterior");
        suspension.setFechaFin(hoy.plusDays(2));
        when(suspensionDocenteRepository.findByIdSuspensionAndAccesoNot(5, eliminado))
                .thenReturn(Optional.of(suspension));
        SuspensionDocenteRequest request = request(10, 11);
        request.setMotivo("Nuevo motivo");
        request.setMotivoDetalle("Detalle nuevo");
        request.setFechaFin(hoy.plusDays(10));

        SuspensionDocenteResponse respuesta = service.editarVigente(5,
                new EditarSuspensionVigenteRequest(request.getMotivo(), request.getMotivoDetalle(), request.getFechaFin()));

        assertEquals("Nuevo motivo", suspension.getMotivo());
        assertEquals("Detalle nuevo", suspension.getMotivoDetalle());
        assertEquals(hoy.plusDays(10), suspension.getFechaFin());
        assertEquals(titular, suspension.getDocente());
        assertEquals(sustituto, suspension.getSustituto());
        assertEquals(hoy, suspension.getFechaInicio());
        assertEquals(activo, suspension.getAcceso());
        assertEquals("Nuevo motivo", respuesta.getMotivo());
        verify(suspensionDocenteRepository).save(suspension);
        verify(docenteRepository, never()).save(any());
    }

    @Test
    void actualizarRechazaCambiosDeDocenteSustitutoOFechaInicio() {
        SuspensionDocente suspension = suspension(titular, sustituto);
        when(suspensionDocenteRepository.findByIdSuspensionAndAccesoNot(5, eliminado))
                .thenReturn(Optional.of(suspension));
        List<SuspensionDocenteRequest> intentos = List.of(
                request(12, 11), request(10, null), requestConFechaInicio(hoy.plusDays(1)));
        List<String> mensajes = List.of(
                "No se puede cambiar el docente",
                "No se puede cambiar el sustituto",
                "No se puede cambiar la fecha de inicio");

        for (int i = 0; i < intentos.size(); i++) {
            final int indice = i;
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> service.actualizar(5, intentos.get(indice)));
            assertTrue(error.getMessage().contains("suspensiones vigentes"), error.getMessage());
        }
        assertEquals(titular, suspension.getDocente());
        assertEquals(sustituto, suspension.getSustituto());
        assertEquals(hoy, suspension.getFechaInicio());
        verify(suspensionDocenteRepository, never()).save(any());
    }

    @Test
    void actualizarRechazaFechaFinAnteriorAFechaInicio() {
        SuspensionDocente suspension = suspension(titular, sustituto);
        when(suspensionDocenteRepository.findByIdSuspensionAndAccesoNot(5, eliminado))
                .thenReturn(Optional.of(suspension));
        SuspensionDocenteRequest request = request(10, 11);
        request.setFechaFin(hoy.minusDays(1));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.editarVigente(5, new EditarSuspensionVigenteRequest("Motivo", null, request.getFechaFin())));

        assertTrue(error.getMessage().contains("La fecha de fin no puede ser anterior"));
        verify(suspensionDocenteRepository, never()).save(any());
    }

    @Test
    void actualizarRechazaSuspensionInactiva() {
        SuspensionDocente suspension = suspension(titular, sustituto);
        suspension.setAcceso(inactivo);
        when(suspensionDocenteRepository.findByIdSuspensionAndAccesoNot(5, eliminado))
                .thenReturn(Optional.of(suspension));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.actualizar(5, request(10, 11)));

        assertTrue(error.getMessage().contains("Solo se pueden editar suspensiones activas"));
    }

    @Test
    void deleteRechazaBorrarUnaSuspensionActiva() {
        SuspensionDocente suspension = suspension(titular, sustituto);
        when(suspensionDocenteRepository.findByIdSuspensionAndAccesoNot(5, eliminado))
                .thenReturn(Optional.of(suspension));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.delete(5));

        assertTrue(error.getMessage().contains("No se puede eliminar una suspensión activa"));
        assertEquals(inactivo, titular.getAcceso());
        assertEquals(activo, suspension.getAcceso());
        verify(suspensionDocenteRepository, never()).save(any());
        verify(docenteRepository, never()).save(any());
    }

    @Test
    void creaProgramadaSinInactivarAlDocente() {
        SuspensionDocenteRequest request = request(10, null);
        request.setFechaInicio(hoy.plusDays(3));

        SuspensionDocenteResponse respuesta = service.create(request);

        assertEquals(EstadoSuspension.PROGRAMADA.name(), respuesta.getEstado());
        assertEquals(activo, titular.getAcceso());
        verify(docenteRepository, never()).save(any());
    }

    @Test
    void programadaSeActivaElDiaCorrespondiente() {
        SuspensionDocente suspension = suspension(titular, null);
        suspension.setEstado(EstadoSuspension.PROGRAMADA);
        suspension.setFechaInicio(hoy);
        titular.setAcceso(activo);
        when(suspensionDocenteRepository.findByIdSuspensionAndAccesoNot(5, eliminado))
                .thenReturn(Optional.of(suspension));

        service.activarProgramado(5, hoy);

        assertEquals(EstadoSuspension.VIGENTE, suspension.getEstado());
        assertEquals(inactivo, titular.getAcceso());
    }

    @Test
    void finalizaAutomaticamenteAlVencerLaFechaFin() {
        SuspensionDocente suspension = suspension(titular, sustituto);
        suspension.setFechaFin(hoy.minusDays(1));
        when(suspensionDocenteRepository.findByIdSuspensionAndAccesoNot(5, eliminado))
                .thenReturn(Optional.of(suspension));

        service.finalizarAutomatico(5, hoy);

        assertEquals(EstadoSuspension.FINALIZADA, suspension.getEstado());
        assertEquals("Finalización automática", suspension.getMotivoResolucion());
        assertEquals(hoy, suspension.getFechaFinalizacion());
        assertEquals(activo, titular.getAcceso());
    }

    @Test
    void noReactivaSiExisteOtraSuspensionVigente() {
        SuspensionDocente suspension = suspension(titular, null);
        suspension.setEstado(EstadoSuspension.VIGENTE);
        when(suspensionDocenteRepository.findByIdSuspensionAndAccesoNot(5, eliminado))
                .thenReturn(Optional.of(suspension));
        when(suspensionDocenteRepository.existsByDocenteIdDocenteAndEstadoInAndAccesoNotAndIdSuspensionNot(
                10, List.of(EstadoSuspension.VIGENTE), eliminado, 5)).thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.finalizar(5, "Motivo"));

        assertTrue(error.getMessage().contains("otra suspensión vigente"));
        assertEquals(inactivo, titular.getAcceso());
        assertEquals(EstadoSuspension.VIGENTE, suspension.getEstado());
        verify(docenteRepository, never()).save(any());
        verify(suspensionDocenteRepository, never()).save(any());
        verify(bloqueoCalificaciones).docente(10);
    }

    @Test
    void finalizarMarcaEstadoYReactivaAlDocente() {
        SuspensionDocente suspension = suspension(titular, null);
        suspension.setEstado(EstadoSuspension.VIGENTE);
        when(suspensionDocenteRepository.findByIdSuspensionAndAccesoNot(5, eliminado))
                .thenReturn(Optional.of(suspension));

        SuspensionDocenteResponse respuesta = service.finalizar(5, "Fin de licencia");

        assertEquals(EstadoSuspension.FINALIZADA.name(), respuesta.getEstado());
        assertEquals("Fin de licencia", suspension.getMotivoResolucion());
        assertFalse(respuesta.isPuedeEditar());
        assertEquals(activo, titular.getAcceso());
    }

    @Test
    void deletePermiteBorrarUnaSuspensionFinalizada() {
        SuspensionDocente suspension = suspension(titular, null);
        suspension.setEstado(EstadoSuspension.FINALIZADA);
        when(suspensionDocenteRepository.findByIdSuspensionAndAccesoNot(5, eliminado))
                .thenReturn(Optional.of(suspension));

        service.delete(5);

        assertEquals(eliminado, suspension.getAcceso());
        assertEquals(inactivo, titular.getAcceso());
    }

    @Test
    void creaRechazaTransferenciaActivaEnElMismoPeriodo() {
        Asignacion asignacion = new Asignacion();
        asignacion.setIdAsignacion(20);
        CambioDocente cambio = new CambioDocente();
        cambio.setEstado(EstadoCambioDocente.VIGENTE);
        cambio.setFechaInicio(hoy);
        cambio.setFechaFin(hoy.plusDays(2));
        CambioDocenteAsignacion detalle = new CambioDocenteAsignacion();
        detalle.setAsignacion(asignacion);
        detalle.setCambio(cambio);
        when(asignacionRepository.findByDocenteIdDocenteAndAccesoNot(10, eliminado))
                .thenReturn(List.of(asignacion));
        when(detalleRepository.findByAsignacionIdAsignacionInAndDevueltaFalseAndAccesoNot(List.of(20), eliminado))
                .thenReturn(List.of(detalle));
        SuspensionDocenteRequest request = request(10, null);
        request.setFechaFin(hoy.plusDays(5));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.create(request));

        assertTrue(error.getMessage().contains("ya participa en un cambio de docente"));
        assertEquals(activo, titular.getAcceso());
    }

    @Test
    void creaRechazaTransferenciaSinFechaFinQueSeSolapa() {
        Asignacion asignacion = new Asignacion();
        asignacion.setIdAsignacion(20);
        CambioDocente cambio = new CambioDocente();
        cambio.setEstado(EstadoCambioDocente.VIGENTE);
        cambio.setFechaInicio(hoy);
        cambio.setFechaFin(null);
        CambioDocenteAsignacion detalle = new CambioDocenteAsignacion();
        detalle.setAsignacion(asignacion);
        detalle.setCambio(cambio);
        when(asignacionRepository.findByDocenteIdDocenteAndAccesoNot(10, eliminado))
                .thenReturn(List.of(asignacion));
        when(detalleRepository.findByAsignacionIdAsignacionInAndDevueltaFalseAndAccesoNot(List.of(20), eliminado))
                .thenReturn(List.of(detalle));
        SuspensionDocenteRequest request = request(10, null);
        request.setFechaInicio(hoy.plusDays(3));
        request.setFechaFin(hoy.plusDays(5));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.create(request));

        assertTrue(error.getMessage().contains("ya participa en un cambio de docente"));
        assertEquals(activo, titular.getAcceso());
    }

    @Test
    void cancelarRechazaSuspensionVigente() {
        SuspensionDocente suspension = suspension(titular, null);
        suspension.setEstado(EstadoSuspension.VIGENTE);
        when(suspensionDocenteRepository.findByIdSuspensionAndAccesoNot(5, eliminado))
                .thenReturn(Optional.of(suspension));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.cancelar(5, null));

        assertTrue(error.getMessage().contains("programada"));
        assertEquals(inactivo, titular.getAcceso());
    }

    @Test
    void finalizarRechazaSuspensionProgramada() {
        SuspensionDocente suspension = suspension(titular, null);
        suspension.setEstado(EstadoSuspension.PROGRAMADA);
        titular.setAcceso(activo);
        when(suspensionDocenteRepository.findByIdSuspensionAndAccesoNot(5, eliminado))
                .thenReturn(Optional.of(suspension));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.finalizar(5));

        assertTrue(error.getMessage().contains("vigente"));
        assertEquals(activo, titular.getAcceso());
    }

    @Test
    void cancelarProgramadaNoModificaElAccesoDelDocente() {
        SuspensionDocente suspension = suspension(titular, null);
        suspension.setEstado(EstadoSuspension.PROGRAMADA);
        titular.setAcceso(activo);
        when(suspensionDocenteRepository.findByIdSuspensionAndAccesoNot(5, eliminado))
                .thenReturn(Optional.of(suspension));

        SuspensionDocenteResponse respuesta = service.cancelar(5, "Error de registro");

        assertEquals(EstadoSuspension.CANCELADA.name(), respuesta.getEstado());
        assertEquals("Error de registro", suspension.getMotivoResolucion());
        assertEquals(activo, titular.getAcceso());
        verify(docenteRepository, never()).save(any());
    }

    @Test
    void respuestaIndicaAccionesSegunEstado() {
        SuspensionDocente programada = suspension(docente(12, "Programada"), null);
        programada.setEstado(EstadoSuspension.PROGRAMADA);

        when(suspensionDocenteRepository.findByAccesoNot(eliminado)).thenReturn(List.of(programada));
        SuspensionDocenteResponse respuestaProgramada = service.getAll().get(0);
        assertTrue(respuestaProgramada.isPuedeEditar());
        assertTrue(respuestaProgramada.isPuedeCancelar());
        assertFalse(respuestaProgramada.isPuedeFinalizar());

        SuspensionDocente vigente = suspension(docente(13, "Vigente"), null);
        vigente.setEstado(EstadoSuspension.VIGENTE);
        when(suspensionDocenteRepository.findByAccesoNot(eliminado)).thenReturn(List.of(vigente));
        SuspensionDocenteResponse respuestaVigente = service.getAll().get(0);
        assertFalse(respuestaVigente.isPuedeCancelar());
        assertTrue(respuestaVigente.isPuedeFinalizar());
    }

    private Acceso acceso(long id) {
        Acceso acceso = new Acceso();
        acceso.setIdAcceso((long) id);
        return acceso;
    }

    private Docente docente(int id, String nombre) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setApellidoPat("Apellido");
        Docente docente = new Docente();
        docente.setIdDocente(id);
        docente.setAcceso(activo);
        docente.setUsuario(usuario);
        return docente;
    }

    private SuspensionDocenteRequest request(int idDocente, Integer idSustituto) {
        SuspensionDocenteRequest request = new SuspensionDocenteRequest();
        request.setIdDocente(idDocente);
        request.setIdSustituto(idSustituto);
        request.setMotivo("Licencia");
        request.setFechaInicio(hoy);
        return request;
    }

    private SuspensionDocenteRequest requestConFechaInicio(LocalDate fechaInicio) {
        SuspensionDocenteRequest request = request(10, 11);
        request.setFechaInicio(fechaInicio);
        return request;
    }

    private SuspensionDocente suspension(Docente docente, Docente sustituto) {
        SuspensionDocente suspension = new SuspensionDocente();
        suspension.setIdSuspension(5);
        suspension.setDocente(docente);
        suspension.setSustituto(sustituto);
        suspension.setMotivo("Licencia");
        suspension.setFechaInicio(hoy);
        suspension.setAcceso(activo);
        docente.setAcceso(inactivo);
        return suspension;
    }
}
