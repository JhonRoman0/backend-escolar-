package com.example.Escolar.Service;

import com.example.Escolar.Dto.SuspensionDocenteRequest;
import com.example.Escolar.Dto.EditarSuspensionVigenteRequest;
import com.example.Escolar.Dto.SuspensionDocenteResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.Asignacion;
import com.example.Escolar.Model.CambioDocente;
import com.example.Escolar.Model.CambioDocenteAsignacion;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.EstadoCambioDocente;
import com.example.Escolar.Model.EstadoSuspension;
import com.example.Escolar.Model.SuspensionDocente;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.CambioDocenteAsignacionRepository;
import com.example.Escolar.Repository.DocenteRepository;
import com.example.Escolar.Repository.SuspensionDocenteRepository;
import com.example.Escolar.Util.FechaColegio;
import com.example.Escolar.Security.ConcesionesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuspensionDocenteService {

    private static final List<EstadoSuspension> ESTADOS_ACTIVOS =
            List.of(EstadoSuspension.PROGRAMADA, EstadoSuspension.VIGENTE);

    private final SuspensionDocenteRepository suspensionDocenteRepository;
    private final DocenteRepository docenteRepository;
    private final CambioDocenteAsignacionRepository detalleRepository;
    private final AsignacionRepository asignacionRepository;
    private final AccesoRepository accesoRepository;
    private final AnioEscolarRepository anioEscolarRepository;
    private final ConcesionesService concesionesService;
    private final BloqueoCalificaciones bloqueoCalificaciones;
    private final CambioDocenteService cambioDocenteService;

    public List<SuspensionDocenteResponse> getAll() {
        return conIncidencias(suspensionDocenteRepository.findByAccesoNot(eliminado()));
    }

    public SuspensionDocenteResponse getById(Integer id) {
        return conIncidencias(List.of(findSuspension(id))).get(0);
    }

    public List<SuspensionDocenteResponse> getByDocente(Integer idDocente) {
        Docente docente = findDocente(idDocente);
        return conIncidencias(suspensionDocenteRepository.findByDocenteAndAccesoNot(docente, eliminado()));
    }

    private List<SuspensionDocenteResponse> conIncidencias(List<SuspensionDocente> suspensiones) {
        List<AnioEscolar> anios = aniosParaRespuestas();
        Map<Integer, String> incidencias = cambioDocenteService.incidenciasDeCobertura(
                suspensiones.stream().filter(s -> s.getSustituto() != null).map(SuspensionDocente::getIdSuspension).toList());
        return suspensiones.stream().map(s -> {
            SuspensionDocenteResponse respuesta = toResponse(s, anios);
            String incidencia = incidencias.get(s.getIdSuspension());
            if (incidencia != null) {
                respuesta.setIncidencia(true);
                respuesta.setMotivoIncidencia(incidencia);
            }
            return respuesta;
        }).toList();
    }

    @Transactional
    public SuspensionDocenteResponse create(SuspensionDocenteRequest request) {
        LocalDate hoy = FechaColegio.hoy();
        Docente docente = findDocente(request.getIdDocente());
        bloqueoCalificaciones.docente(docente.getIdDocente());
        validarDocenteSuspendible(docente);
        validarSuspensionDuplicada(docente);
        validarFechas(request.getFechaInicio(), request.getFechaFin());

        Docente sustituto = validarSustituto(request.getIdSustituto());
        validarTransferenciasDuplicadas(docente, request.getFechaInicio(), request.getFechaFin());

        EstadoSuspension estado = request.getFechaInicio().isAfter(hoy)
                ? EstadoSuspension.PROGRAMADA : EstadoSuspension.VIGENTE;

        SuspensionDocente suspension = new SuspensionDocente();
        suspension.setDocente(docente);
        suspension.setSustituto(sustituto);
        suspension.setMotivo(request.getMotivo());
        suspension.setMotivoDetalle(request.getMotivoDetalle());
        suspension.setFechaInicio(request.getFechaInicio());
        suspension.setFechaFin(request.getFechaFin());
        suspension.setEstado(estado);
        suspension.setAcceso(activo());
        suspension = suspensionDocenteRepository.save(suspension);
        if (sustituto != null) cambioDocenteService.prepararCobertura(suspension, hoy);
        if (estado == EstadoSuspension.VIGENTE) activar(suspension, hoy, false);
        return toResponse(suspension);
    }

    @Transactional
    public SuspensionDocenteResponse actualizar(Integer id, SuspensionDocenteRequest request) {
        SuspensionDocente suspension = findSuspension(id);
        bloqueoCalificaciones.suspension(id);
        validarEditable(suspension);
        if (suspension.getEstado() != EstadoSuspension.PROGRAMADA) {
            throw new IllegalArgumentException("Usa la edicion de suspensiones vigentes.");
        }
        bloqueoCalificaciones.docente(suspension.getDocente().getIdDocente());
        validarFechas(request.getFechaInicio(), request.getFechaFin());

        Docente docente = suspension.getDocente();
        if (!Objects.equals(docente.getIdDocente(), request.getIdDocente())) {
            throw new IllegalArgumentException("No se puede cambiar el docente de una suspensión activa");
        }
        Integer sustitutoActual = suspension.getSustituto() == null
                ? null : suspension.getSustituto().getIdDocente();
        if (!Objects.equals(sustitutoActual, request.getIdSustituto())) {
            throw new IllegalArgumentException("No se puede cambiar el sustituto de una suspensión activa");
        }
        if (!Objects.equals(suspension.getFechaInicio(), request.getFechaInicio())) {
            throw new IllegalArgumentException("No se puede cambiar la fecha de inicio de una suspensión activa");
        }
        if (request.getMotivo() == null || request.getMotivo().isBlank()) {
            throw new IllegalArgumentException("El motivo es obligatorio");
        }
        validarTransferenciasDuplicadas(docente, suspension.getFechaInicio(), request.getFechaFin());

        suspension.setMotivo(request.getMotivo());
        suspension.setMotivoDetalle(request.getMotivoDetalle());
        suspension.setFechaFin(request.getFechaFin());
        if (suspension.getEstado() == EstadoSuspension.PROGRAMADA
                && !suspension.getFechaInicio().isAfter(FechaColegio.hoy())) {
            activar(suspension, FechaColegio.hoy(), false);
        }
        return toResponse(suspensionDocenteRepository.save(suspension));
    }

    @Transactional
    public SuspensionDocenteResponse editarVigente(Integer id, EditarSuspensionVigenteRequest request) {
        SuspensionDocente suspension = findSuspension(id);
        bloqueoCalificaciones.suspension(id);
        bloqueoCalificaciones.docente(suspension.getDocente().getIdDocente());
        validarEditable(suspension);
        if (suspension.getEstado() != EstadoSuspension.VIGENTE) {
            throw new IllegalArgumentException("Solo se pueden editar suspensiones vigentes por esta ruta.");
        }
        validarFechas(suspension.getFechaInicio(), request.fechaFin());
        validarTransferenciasDuplicadas(suspension.getDocente(), suspension.getFechaInicio(), request.fechaFin());
        suspension.setMotivo(request.motivo());
        suspension.setMotivoDetalle(request.motivoDetalle());
        suspension.setFechaFin(request.fechaFin());
        return toResponse(suspensionDocenteRepository.save(suspension));
    }

    @Transactional
    public SuspensionDocenteResponse finalizar(Integer id) {
        return finalizar(id, null);
    }

    @Transactional
    public SuspensionDocenteResponse finalizar(Integer id, String motivo) {
        return cerrar(id, EstadoSuspension.FINALIZADA,
                motivo == null || motivo.isBlank() ? "Finalizada por el administrador" : motivo.trim(),
                FechaColegio.hoy());
    }

    @Transactional
    public SuspensionDocenteResponse cancelar(Integer id, String motivo) {
        return cerrar(id, EstadoSuspension.CANCELADA,
                motivo == null || motivo.isBlank() ? "Cancelada por el administrador" : motivo.trim(),
                FechaColegio.hoy());
    }

    @Transactional
    public void delete(Integer id) {
        SuspensionDocente suspension = findSuspension(id);
        bloqueoCalificaciones.suspension(id);
        if (ESTADOS_ACTIVOS.contains(suspension.getEstado())) {
            throw new IllegalArgumentException(
                    "No se puede eliminar una suspensión activa; use finalizar para reactivar al docente");
        }
        suspension.setAcceso(eliminado());
        suspensionDocenteRepository.save(suspension);
    }

    public List<Integer> idsProgramadosActivables(LocalDate hoy) {
        return suspensionDocenteRepository.findByEstadoAndFechaInicioLessThanEqualAndAccesoNot(
                        EstadoSuspension.PROGRAMADA, hoy, eliminado())
                .stream()
                .map(SuspensionDocente::getIdSuspension)
                .toList();
    }

    public List<Integer> idsVigentesFinalizables(LocalDate hoy) {
        return suspensionDocenteRepository.findByEstadoAndFechaFinLessThanAndAccesoNot(
                        EstadoSuspension.VIGENTE, hoy, eliminado())
                .stream()
                .map(SuspensionDocente::getIdSuspension)
                .toList();
    }

    @Transactional
    public void activarProgramado(Integer id, LocalDate hoy) {
        SuspensionDocente suspension = findSuspension(id);
        bloqueoCalificaciones.suspension(id);
        if (suspension.getEstado() != EstadoSuspension.PROGRAMADA
                || suspension.getFechaInicio().isAfter(hoy)) {
            return;
        }
        bloqueoCalificaciones.docente(suspension.getDocente().getIdDocente());
        validarDocenteSuspendible(suspension.getDocente());
        activar(suspension, hoy, true);
        suspensionDocenteRepository.save(suspension);
    }

    @Transactional
    public void finalizarAutomatico(Integer id, LocalDate hoy) {
        SuspensionDocente suspension = findSuspension(id);
        bloqueoCalificaciones.suspension(id);
        if (suspension.getEstado() != EstadoSuspension.VIGENTE
                || suspension.getFechaFin() == null || !suspension.getFechaFin().isBefore(hoy)) {
            return;
        }
        cerrarBloqueada(suspension, EstadoSuspension.FINALIZADA, "Finalización automática", hoy, true);
    }

    private SuspensionDocenteResponse cerrar(Integer id, EstadoSuspension estado,
                                             String motivo, LocalDate hoy) {
        return cerrar(id, estado, motivo, hoy, false);
    }

    private SuspensionDocenteResponse cerrar(Integer id, EstadoSuspension estado,
                                              String motivo, LocalDate hoy, boolean automatica) {
        SuspensionDocente suspension = findSuspension(id);
        bloqueoCalificaciones.suspension(id);
        return cerrarBloqueada(suspension, estado, motivo, hoy, automatica);
    }

    private SuspensionDocenteResponse cerrarBloqueada(SuspensionDocente suspension, EstadoSuspension estado,
                                                      String motivo, LocalDate hoy, boolean automatica) {
        validarEditable(suspension);

        if (estado == EstadoSuspension.CANCELADA
                && suspension.getEstado() != EstadoSuspension.PROGRAMADA) {
            throw new IllegalArgumentException("Solo se puede cancelar una suspensión programada");
        }
        if (estado == EstadoSuspension.FINALIZADA
                && suspension.getEstado() != EstadoSuspension.VIGENTE) {
            throw new IllegalArgumentException("Solo se puede finalizar una suspensión vigente");
        }
        bloqueoCalificaciones.docente(suspension.getDocente().getIdDocente());
        if (suspension.getSustituto() != null) {
            if (!cambioDocenteService.cerrarCobertura(suspension, hoy, automatica)) {
                return conIncidencias(List.of(suspension)).get(0);
            }
            validarCambiosActivos(suspension.getSustituto());
        }
        if (suspension.getEstado() == EstadoSuspension.VIGENTE) {
            reactivar(suspension);
        }
        suspension.setEstado(estado);
        suspension.setFechaFinalizacion(hoy);
        suspension.setMotivoResolucion(motivo);
        return toResponse(suspensionDocenteRepository.save(suspension));
    }

    private void activar(SuspensionDocente suspension, LocalDate hoy, boolean automatica) {
        if (suspension.getSustituto() != null) cambioDocenteService.activarCobertura(suspension, hoy, automatica);
        inactivar(suspension.getDocente());
        invalidarVinculos(suspension);
        suspension.setEstado(EstadoSuspension.VIGENTE);
        suspension.setFechaFinalizacion(null);
        suspension.setMotivoResolucion(null);
    }

    private void inactivar(Docente docente) {
        validarUsuarioNoEliminado(docente);
        docente.setAcceso(inactivo());
        docenteRepository.save(docente);
    }

    private void reactivar(SuspensionDocente suspension) {
        Docente docente = suspension.getDocente();
        if (!AccesoConstants.INACTIVO.equals(docente.getAcceso().getIdAcceso())) {
            return;
        }
        validarUsuarioNoEliminado(docente);
        boolean otraVigente = suspensionDocenteRepository
                .existsByDocenteIdDocenteAndEstadoInAndAccesoNotAndIdSuspensionNot(
                        docente.getIdDocente(), List.of(EstadoSuspension.VIGENTE),
                        eliminado(), suspension.getIdSuspension());
        if (otraVigente) {
            throw new IllegalArgumentException("El docente tiene otra suspensión vigente; no se puede reactivar");
        }
        docente.setAcceso(activo());
        docenteRepository.save(docente);
        invalidarVinculos(suspension);
    }

    private void invalidarVinculos(SuspensionDocente suspension) {
        Set<Integer> ids = new LinkedHashSet<>();
        if (suspension.getDocente() != null && suspension.getDocente().getUsuario() != null) {
            ids.add(suspension.getDocente().getUsuario().getIdUsuario());
        }
        if (suspension.getSustituto() != null && suspension.getSustituto().getUsuario() != null) {
            ids.add(suspension.getSustituto().getUsuario().getIdUsuario());
        }
        concesionesService.invalidarVinculoDespuesDeCommit(ids);
    }

    private void validarDocenteSuspendible(Docente docente) {
        if (!AccesoConstants.ACTIVO.equals(docente.getAcceso().getIdAcceso())) {
            throw new IllegalArgumentException("Solo se pueden suspender docentes en estado ACTIVO");
        }
        validarUsuarioNoEliminado(docente);
    }

    private void validarUsuarioNoEliminado(Docente docente) {
        Usuario usuario = docente.getUsuario();
        if (usuario != null && usuario.getAcceso() != null
                && AccesoConstants.ELIMINADO.equals(usuario.getAcceso().getIdAcceso())) {
            throw new IllegalArgumentException("El usuario del docente está eliminado");
        }
    }

    private void validarSuspensionDuplicada(Docente docente) {
        boolean duplicada = suspensionDocenteRepository.existsByDocenteAndEstadoInAndAccesoNot(
                docente, ESTADOS_ACTIVOS, eliminado());
        if (duplicada) {
            throw new IllegalArgumentException("El docente ya tiene una suspensión activa");
        }
    }

    private void validarFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaFin != null && fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
    }

    private Docente validarSustituto(Integer idSustituto) {
        if (idSustituto == null) {
            return null;
        }
        Docente sustituto = findDocente(idSustituto);
        if (!AccesoConstants.ACTIVO.equals(sustituto.getAcceso().getIdAcceso())) {
            throw new IllegalArgumentException("El sustituto debe estar en estado ACTIVO");
        }
        if (suspensionDocenteRepository.existsByDocenteAndEstadoInAndAccesoNot(
                sustituto, ESTADOS_ACTIVOS, eliminado())) {
            throw new IllegalArgumentException("El sustituto tiene una suspensión activa");
        }
        validarCambiosActivos(sustituto);
        return sustituto;
    }

    private void validarCambiosActivos(Docente sustituto) {
        boolean pendiente = detalleRepository
                .findByDocenteHaciaIdDocenteAndDevueltaFalseAndAccesoNot(sustituto.getIdDocente(), eliminado())
                .stream()
                .map(CambioDocenteAsignacion::getCambio)
                .filter(Objects::nonNull)
                .anyMatch(c -> EstadoCambioDocente.ACTIVOS.contains(c.getEstado()));
        if (pendiente) {
            throw new IllegalArgumentException(
                    "El docente aún tiene asignaciones pendientes de devolución en Cambios de docente");
        }
    }

    private void validarTransferenciasDuplicadas(Docente docente, LocalDate fechaInicio, LocalDate fechaFin) {
        List<Integer> ids = asignacionRepository.findByDocenteIdDocenteAndAccesoNot(
                        docente.getIdDocente(), eliminado())
                .stream()
                .map(Asignacion::getIdAsignacion)
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        for (CambioDocenteAsignacion detalle : detalleRepository
                .findByAsignacionIdAsignacionInAndDevueltaFalseAndAccesoNot(ids, eliminado())) {
            CambioDocente cambio = detalle.getCambio();
            if (cambio == null || !EstadoCambioDocente.ACTIVOS.contains(cambio.getEstado())) {
                continue;
            }
            if (seSolapan(fechaInicio, fechaFin, cambio.getFechaInicio(), cambio.getFechaFin())) {
                throw new IllegalArgumentException("La asignación #" + detalle.getAsignacion().getIdAsignacion()
                        + " ya participa en un cambio de docente pendiente en el periodo solicitado");
            }
        }
    }

    private boolean seSolapan(LocalDate inicioA, LocalDate finA, LocalDate inicioB, LocalDate finB) {
        if (inicioA == null || inicioB == null) {
            return false;
        }
        boolean empiezaAntesDelFinB = finB == null || !inicioA.isAfter(finB);
        boolean empiezaAntesDelFinA = finA == null || !inicioB.isAfter(finA);
        return empiezaAntesDelFinB && empiezaAntesDelFinA;
    }

    private void validarEditable(SuspensionDocente suspension) {
        if (!ESTADOS_ACTIVOS.contains(suspension.getEstado())
                || !AccesoConstants.ACTIVO.equals(suspension.getAcceso().getIdAcceso())) {
            throw new IllegalArgumentException("Solo se pueden editar suspensiones activas");
        }
    }

    private SuspensionDocente findSuspension(Integer id) {
        return suspensionDocenteRepository.findByIdSuspensionAndAccesoNot(id, eliminado())
                .orElseThrow(() -> new ResourceNotFoundException("Suspensión no encontrada con id " + id));
    }

    private Docente findDocente(Integer id) {
        return docenteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Docente no encontrado con id " + id));
    }

    private com.example.Escolar.Model.Acceso activo() {
        return accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow();
    }

    private com.example.Escolar.Model.Acceso inactivo() {
        return accesoRepository.findById(AccesoConstants.INACTIVO).orElseThrow();
    }

    private com.example.Escolar.Model.Acceso eliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }

    private List<AnioEscolar> aniosParaRespuestas() {
        return anioEscolarRepository.findByAccesoNot(eliminado());
    }

    private String anioDe(LocalDate fecha, List<AnioEscolar> anios) {
        if (fecha == null) {
            return null;
        }
        return anios.stream()
                .filter(a -> a.getFechaInicio() != null && !fecha.isBefore(a.getFechaInicio()))
                .filter(a -> a.getFechaFin() == null || !fecha.isAfter(a.getFechaFin()))
                .findFirst()
                .map(a -> a.getAnio())
                .orElse(null);
    }

    private SuspensionDocenteResponse toResponse(SuspensionDocente suspension) {
        return toResponse(suspension, aniosParaRespuestas());
    }

    private SuspensionDocenteResponse toResponse(SuspensionDocente suspension, List<AnioEscolar> anios) {
        SuspensionDocenteResponse response = new SuspensionDocenteResponse();
        response.setIdSuspension(suspension.getIdSuspension());
        response.setIdDocente(suspension.getDocente().getIdDocente());
        response.setDocente(suspension.getDocente().getUsuario().getNombre() + " "
                + suspension.getDocente().getUsuario().getApellidoPat());
        if (suspension.getSustituto() != null) {
            response.setIdSustituto(suspension.getSustituto().getIdDocente());
            response.setSustituto(suspension.getSustituto().getUsuario().getNombre() + " "
                    + suspension.getSustituto().getUsuario().getApellidoPat());
        }
        response.setMotivo(suspension.getMotivo());
        response.setMotivoDetalle(suspension.getMotivoDetalle());
        response.setFechaInicio(suspension.getFechaInicio());
        response.setFechaFin(suspension.getFechaFin());
        response.setAnio(anioDe(suspension.getFechaInicio(), anios));
        response.setEstado(suspension.getEstado() == null ? null : suspension.getEstado().name());
        response.setFechaFinalizacion(suspension.getFechaFinalizacion());
        response.setMotivoResolucion(suspension.getMotivoResolucion());
        boolean activa = AccesoConstants.ACTIVO.equals(suspension.getAcceso().getIdAcceso());
        response.setPuedeEditar(activa && (suspension.getEstado() == EstadoSuspension.PROGRAMADA
                || suspension.getEstado() == EstadoSuspension.VIGENTE));
        response.setPuedeCancelar(activa && suspension.getEstado() == EstadoSuspension.PROGRAMADA);
        response.setPuedeFinalizar(activa && suspension.getEstado() == EstadoSuspension.VIGENTE);
        response.setAccesoId(suspension.getAcceso().getIdAcceso().longValue());
        return response;
    }
}
