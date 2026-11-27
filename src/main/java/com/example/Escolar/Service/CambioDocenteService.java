package com.example.Escolar.Service;

import com.example.Escolar.Dto.AsignarSustitutoRequest;
import com.example.Escolar.Dto.CambioDocenteRequest;
import com.example.Escolar.Dto.CambioDocenteResponse;
import com.example.Escolar.Dto.EditarCambioVigenteRequest;
import com.example.Escolar.Dto.ResolverIncidenciaRequest;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.Asignacion;
import com.example.Escolar.Model.CambioDocente;
import com.example.Escolar.Model.CambioDocenteAsignacion;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.EstadoCambioDocente;
import com.example.Escolar.Model.EstadoSuspension;
import com.example.Escolar.Model.HorarioClase;
import com.example.Escolar.Model.MotivoCambioDocente;
import com.example.Escolar.Model.OrigenOperacion;
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
import com.example.Escolar.Util.FechaColegio;
import com.example.Escolar.Security.ConcesionesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CambioDocenteService {

    private final CambioDocenteRepository cambioRepository;
    private final CambioDocenteAsignacionRepository detalleRepository;
    private final MotivoCambioDocenteRepository motivoRepository;
    private final AsignacionRepository asignacionRepository;
    private final DocenteRepository docenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final SuspensionDocenteRepository suspensionRepository;
    private final HorarioClaseRepository horarioClaseRepository;
    private final AccesoRepository accesoRepository;
    private final AccesoContextoService contexto;
    private final ConcesionesService concesionesService;
    private final BloqueoCalificaciones bloqueo;

    @Transactional(readOnly = true)
    public List<CambioDocenteResponse> getAll() {
        return toResponses(cambioRepository.findByAccesoNot(eliminado()).stream()
                .filter(c -> c.getSuspension() == null).toList());
    }

    @Transactional(readOnly = true)
    public CambioDocenteResponse getById(Integer id) {
        return toResponse(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<CambioDocenteResponse> getByDocente(Integer idDocente) {
        Docente docente = findDocente(idDocente);
        return toResponses(cambioRepository.findByDocenteAnteriorAndAccesoNot(docente, eliminado()).stream()
                .filter(c -> c.getSuspension() == null).toList());
    }

    public List<Integer> idsProgramadosActivables(LocalDate hoy) {
        return cambioRepository.findByEstadoAndFechaInicioLessThanEqualAndAccesoNot(
                        EstadoCambioDocente.PROGRAMADO, hoy, eliminado())
                .stream()
                .filter(c -> c.getSuspension() == null)
                .map(CambioDocente::getIdCambio)
                .toList();
    }

    public List<Integer> idsTemporalesFinalizables(LocalDate hoy) {
        return cambioRepository.findByEstadoAndIncidenciaFalseAndFechaFinLessThanAndAccesoNot(
                        EstadoCambioDocente.VIGENTE, hoy, eliminado())
                .stream()
                .filter(c -> c.getTipo() == TipoCambioDocente.TEMPORAL)
                .filter(c -> c.getSuspension() == null)
                .map(CambioDocente::getIdCambio)
                .toList();
    }

    @Transactional
    public CambioDocenteResponse create(CambioDocenteRequest request) {
        LocalDate hoy = FechaColegio.hoy();
        Usuario usuario = usuarioAutenticado();
        MotivoCambioDocente motivo = validarMotivo(request);
        List<Asignacion> asignaciones = validarAsignaciones(request);
        List<Integer> ids = asignaciones.stream().map(a -> a.getIdAsignacion()).toList();
        Docente titular = asignaciones.get(0).getDocente();
        Docente sustituto = validarSustituto(request, titular);

        bloquear(ids, titular, sustituto);
        validarCompatibilidadHorarios(sustituto, asignaciones.get(0).getAnioEscolar(), ids);
        validarIncompatibilidad(ids, null);

        CambioDocente cambio = new CambioDocente();
        cambio.setTipo(request.tipo());
        cambio.setEstado(estadoInicial(request, hoy));
        cambio.setAnioEscolar(asignaciones.get(0).getAnioEscolar());
        cambio.setFechaInicio(request.fechaInicio());
        cambio.setFechaFin(request.tipo() == TipoCambioDocente.TEMPORAL ? request.fechaFin() : null);
        cambio.setDocenteAnterior(titular);
        cambio.setDocenteNuevo(sustituto);
        cambio.setMotivo(motivo.getNombre());
        cambio.setMotivoDetalle(request.motivoDetalle());
        cambio.setFechaCambio(hoy);
        cambio.setUsuarioRegistro(usuario);
        cambio.setAcceso(activo());
        cambio.setMotivoRelacionado(motivo);
        cambio.setOrigenRegistro(OrigenOperacion.MANUAL);
        cambio.setOrigenUltimaOperacion(OrigenOperacion.MANUAL);
        cambio.setUsuarioUltimaOperacion(usuario);
        cambio.setIncidencia(false);
        if (cambio.getEstado() == EstadoCambioDocente.APLICADO) {
            cambio.setFechaAplicacion(hoy);
        }

        cambio = cambioRepository.save(cambio);
        List<CambioDocenteAsignacion> detalles = guardarDetalles(cambio, asignaciones, titular, sustituto);
        if (cambio.getEstado() != EstadoCambioDocente.PROGRAMADO) {
            transferir(cambio, usuario, OrigenOperacion.MANUAL, hoy, detalles);
        }
        return toResponse(cambio);
    }

    @Transactional
    public CambioDocenteResponse actualizar(Integer id, CambioDocenteRequest request) {
        CambioDocente cambio = buscar(id);
        EstadoCambioDocente estado = cambio.getEstado();
        if (estado != EstadoCambioDocente.PROGRAMADO) {
            throw new IllegalArgumentException("Solo se pueden editar cambios programados por esta ruta");
        }
        Usuario usuario = usuarioAutenticado();
        LocalDate hoy = FechaColegio.hoy();

        Map<Integer, CambioDocenteAsignacion> detallesPorAsignacion = new LinkedHashMap<>();
        for (CambioDocenteAsignacion detalle : findDetalles(id)) {
            Integer idAsignacion = detalle.getAsignacion().getIdAsignacion();
            if (detallesPorAsignacion.putIfAbsent(idAsignacion, detalle) != null) {
                throw new IllegalArgumentException("La asignación #" + idAsignacion
                        + " está duplicada en el cambio");
            }
        }
        MotivoCambioDocente motivo = validarMotivo(request);
        List<Asignacion> asignaciones = validarAsignaciones(request);
        Docente titular = asignaciones.get(0).getDocente();
        Docente sustituto = validarSustituto(request, titular);
        List<Integer> ids = asignaciones.stream().map(Asignacion::getIdAsignacion).sorted().toList();

        bloquear(ids, titular, sustituto);
        validarCompatibilidadHorarios(sustituto, asignaciones.get(0).getAnioEscolar(), ids);
        validarIncompatibilidad(ids, id);

        List<CambioDocenteAsignacion> detalles =
                sincronizarDetalles(cambio, asignaciones, titular, sustituto, detallesPorAsignacion);

        EstadoCambioDocente estadoCalculado = estadoInicial(request, hoy);
        cambio.setTipo(request.tipo());
        cambio.setEstado(estadoCalculado);
        cambio.setAnioEscolar(asignaciones.get(0).getAnioEscolar());
        cambio.setFechaInicio(request.fechaInicio());
        cambio.setFechaFin(request.tipo() == TipoCambioDocente.TEMPORAL ? request.fechaFin() : null);
        cambio.setDocenteAnterior(titular);
        cambio.setDocenteNuevo(sustituto);
        cambio.setMotivo(motivo.getNombre());
        cambio.setMotivoRelacionado(motivo);
        cambio.setMotivoDetalle(request.motivoDetalle());
        cambio.setOrigenUltimaOperacion(OrigenOperacion.MANUAL);
        cambio.setUsuarioUltimaOperacion(usuario);
        cambio = cambioRepository.save(cambio);

        if (estadoCalculado != EstadoCambioDocente.PROGRAMADO) {
            activar(cambio, usuario, OrigenOperacion.MANUAL, hoy, detalles);
        }
        return toResponse(cambio);
    }

    /** Edición segura de un cambio ya vigente: nunca reactiva ni mueve asignaciones. */
    @Transactional
    public CambioDocenteResponse editarVigente(Integer id, EditarCambioVigenteRequest request) {
        CambioDocente cambio = buscar(id);
        if (cambio.getEstado() != EstadoCambioDocente.VIGENTE) {
            throw new IllegalArgumentException("Solo se pueden editar cambios VIGENTE");
        }
        MotivoCambioDocente motivo = validarMotivoYFechas(
                request.idMotivo(), cambio.getTipo(), cambio.getFechaInicio(), request.fechaFin());
        cambio.setMotivo(motivo.getNombre());
        cambio.setMotivoRelacionado(motivo);
        cambio.setMotivoDetalle(request.motivoDetalle());
        cambio.setFechaFin(cambio.getTipo() == TipoCambioDocente.TEMPORAL ? request.fechaFin() : null);
        cambio.setOrigenUltimaOperacion(OrigenOperacion.MANUAL);
        cambio.setUsuarioUltimaOperacion(usuarioAutenticado());
        return toResponse(cambioRepository.save(cambio));
    }

    private List<CambioDocenteAsignacion> sincronizarDetalles(
            CambioDocente cambio, List<Asignacion> asignaciones, Docente titular,
            Docente sustituto, Map<Integer, CambioDocenteAsignacion> detallesPorAsignacion) {
        Set<Integer> idsSolicitados = asignaciones.stream()
                .map(Asignacion::getIdAsignacion)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<CambioDocenteAsignacion> vigentes = new ArrayList<>();
        for (Asignacion asignacion : asignaciones) {

            CambioDocenteAsignacion detalle = detallesPorAsignacion.get(asignacion.getIdAsignacion());
            if (detalle == null) {
                detalle = new CambioDocenteAsignacion();
                detalle.setCambio(cambio);
                detalle.setAsignacion(asignacion);
                detalle.setDevuelta(false);
                detalle.setAcceso(activo());
            }
            detalle.setDocenteDesde(titular);
            detalle.setDocenteHacia(sustituto);
            detalle.setSinCobertura(sustituto == null);
            detalleRepository.save(detalle);
            vigentes.add(detalle);
        }
        for (Map.Entry<Integer, CambioDocenteAsignacion> entrada : detallesPorAsignacion.entrySet()) {
            if (!idsSolicitados.contains(entrada.getKey())) {
                CambioDocenteAsignacion detalle = entrada.getValue();
                detalle.setAcceso(eliminado());
                detalleRepository.save(detalle);
            }
        }
        return vigentes;
    }

    @Transactional
    public CambioDocenteResponse asignarSustituto(Integer id, AsignarSustitutoRequest request) {
        CambioDocente cambio = buscar(id);
        if (cambio.getEstado() != EstadoCambioDocente.VIGENTE) {
            throw new IllegalArgumentException("Solo se puede asignar un sustituto a un cambio vigente");
        }
        validarAnioVigente(cambio.getAnioEscolar());
        Docente sustituto = validarSustitutoDocente(request.idDocenteSustituto(), cambio.getDocenteAnterior());
        List<Integer> ids = findDetalles(id).stream()
                .filter(d -> Boolean.TRUE.equals(d.getSinCobertura()) && !Boolean.TRUE.equals(d.getDevuelta()))
                .map(d -> d.getAsignacion().getIdAsignacion())
                .sorted()
                .toList();
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("El cambio no tiene asignaciones sin cobertura");
        }
        bloquear(ids, cambio.getDocenteAnterior(), sustituto);
        validarCompatibilidadHorarios(sustituto, cambio.getAnioEscolar(), ids);
        List<CambioDocenteAsignacion> detalles = findDetalles(id).stream()
                .filter(d -> Boolean.TRUE.equals(d.getSinCobertura()) && !Boolean.TRUE.equals(d.getDevuelta()))
                .toList();
        Usuario usuario = usuarioAutenticado();
        LocalDate hoy = FechaColegio.hoy();

        cambio.setDocenteNuevo(sustituto);
        cambio.setOrigenUltimaOperacion(OrigenOperacion.MANUAL);
        cambio.setUsuarioUltimaOperacion(usuario);
        for (CambioDocenteAsignacion detalle : detalles) {
            Asignacion asignacion = detalle.getAsignacion();
            asignacion.setDocente(sustituto);
            asignacionRepository.save(asignacion);
            detalle.setDocenteHacia(sustituto);
            detalle.setSinCobertura(false);
            detalle.setFechaTransferencia(hoy);
            detalle.setUsuarioTransferencia(usuario);
            detalle.setOrigenTransferencia(OrigenOperacion.MANUAL);
            detalleRepository.save(detalle);
        }
        return toResponse(cambioRepository.save(cambio));
    }

    @Transactional
    public CambioDocenteResponse resolverIncidencia(Integer id, ResolverIncidenciaRequest request) {
        CambioDocente cambio = buscar(id);
        if (!Boolean.TRUE.equals(cambio.getIncidencia())) {
            throw new IllegalArgumentException("El cambio no tiene una incidencia registrada");
        }
        Usuario usuario = usuarioAutenticado();
        cambio.setResolucionAdministrativa(request.resolucion().trim());
        cambio.setUsuarioResolucion(usuario);
        cambio.setFechaResolucion(FechaColegio.hoy());
        cambio.setOrigenUltimaOperacion(OrigenOperacion.MANUAL);
        cambio.setUsuarioUltimaOperacion(usuario);
        return toResponse(cambioRepository.save(cambio));
    }

    @Transactional
    public CambioDocenteResponse finalizarManual(Integer id) {
        CambioDocente cambio = buscar(id);
        if (cambio.getEstado() != EstadoCambioDocente.VIGENTE) {
            throw new IllegalArgumentException("Solo se puede finalizar un cambio vigente");
        }
        Usuario usuario = usuarioAutenticado();
        finalizarConDevolucion(cambio, usuario, OrigenOperacion.MANUAL, FechaColegio.hoy(), true);
        return toResponse(cambio);
    }

    @Transactional
    public CambioDocenteResponse cancelar(Integer id, String motivo) {
        CambioDocente cambio = buscarBloqueado(id);
        if (cambio.getEstado() != EstadoCambioDocente.PROGRAMADO) {
            throw new IllegalArgumentException("Solo se puede cancelar un cambio programado");
        }
        Usuario usuario = usuarioAutenticado();
        cambio.setEstado(EstadoCambioDocente.CANCELADO);
        cambio.setMotivoResolucion(motivo == null || motivo.isBlank()
                ? "Cancelado por el administrador" : motivo.trim());
        cambio.setOrigenUltimaOperacion(OrigenOperacion.MANUAL);
        cambio.setUsuarioUltimaOperacion(usuario);
        return toResponse(cambioRepository.save(cambio));
    }

    @Transactional
    public void activarProgramado(Integer id, LocalDate hoy) {
        CambioDocente cambio = buscarBloqueado(id);
        if (cambio.getEstado() != EstadoCambioDocente.PROGRAMADO || cambio.getFechaInicio().isAfter(hoy)) {
            return;
        }
        activar(cambio, null, OrigenOperacion.AUTOMATICO, hoy);
    }

    private void activar(CambioDocente cambio, Usuario usuario, OrigenOperacion origen, LocalDate hoy) {
        prepararActivacion(cambio, findDetalles(cambio.getIdCambio()));
        transferir(cambio, usuario, origen, hoy);
        confirmarActivacion(cambio, usuario, origen, hoy);
    }

    private void activar(CambioDocente cambio, Usuario usuario, OrigenOperacion origen, LocalDate hoy,
                         List<CambioDocenteAsignacion> detalles) {
        prepararActivacion(cambio, detalles);
        transferir(cambio, usuario, origen, hoy, detalles);
        confirmarActivacion(cambio, usuario, origen, hoy);
    }

    private void prepararActivacion(CambioDocente cambio, List<CambioDocenteAsignacion> detalles) {
        List<Integer> ids = detalles.stream().map(d -> d.getAsignacion().getIdAsignacion()).sorted().toList();
        if (cambio.getDocenteNuevo() == null) {
            throw new IllegalArgumentException("El cambio programado no tiene sustituto asignado");
        }
        bloquear(ids, cambio.getDocenteAnterior(), cambio.getDocenteNuevo());
        validarCompatibilidadHorarios(cambio.getDocenteNuevo(), cambio.getAnioEscolar(), ids);
    }

    private void confirmarActivacion(CambioDocente cambio, Usuario usuario, OrigenOperacion origen, LocalDate hoy) {
        if (cambio.getTipo() == TipoCambioDocente.DEFINITIVO) {
            cambio.setEstado(EstadoCambioDocente.APLICADO);
            cambio.setFechaAplicacion(hoy);
        } else {
            cambio.setEstado(EstadoCambioDocente.VIGENTE);
        }
        cambio.setOrigenUltimaOperacion(origen);
        cambio.setUsuarioUltimaOperacion(usuario);
        cambioRepository.save(cambio);
    }

    @Transactional
    public void finalizarAutomatico(Integer id, LocalDate hoy) {
        CambioDocente cambio = buscar(id);
        if (cambio.getEstado() != EstadoCambioDocente.VIGENTE
                || cambio.getFechaFin() == null || !cambio.getFechaFin().isBefore(hoy)
                || Boolean.TRUE.equals(cambio.getIncidencia())) {
            return;
        }
        finalizarConDevolucion(cambio, null, OrigenOperacion.AUTOMATICO, hoy, false);
    }

    void prepararCobertura(SuspensionDocente suspension, LocalDate hoy) {
        Docente titular = suspension.getDocente();
        Docente sustituto = validarSustitutoDocente(suspension.getSustituto().getIdDocente(), titular);
        CambioDocente cambio = new CambioDocente();
        cambio.setSuspension(suspension);
        cambio.setDocenteAnterior(titular);
        cambio.setDocenteNuevo(sustituto);
        cambio.setTipo(TipoCambioDocente.TEMPORAL);
        cambio.setEstado(EstadoCambioDocente.PROGRAMADO);
        cambio.setFechaCambio(hoy);
        cambio.setFechaInicio(suspension.getFechaInicio());
        cambio.setMotivo(suspension.getMotivo());
        cambio.setUsuarioRegistro(usuarioAutenticado());
        cambio.setOrigenRegistro(OrigenOperacion.MANUAL);
        cambio.setOrigenUltimaOperacion(OrigenOperacion.MANUAL);
        cambio.setAcceso(activo());
        cambio.setIncidencia(false);
        cambioRepository.save(cambio);
    }

    void activarCobertura(SuspensionDocente suspension, LocalDate hoy, boolean automatica) {
        CambioDocente cambio = cambioRepository.findBySuspensionIdSuspension(suspension.getIdSuspension())
                .orElseThrow(() -> new IllegalArgumentException("La suspension no tiene cobertura registrada."));
        Docente titular = suspension.getDocente();
        Docente sustituto = validarSustitutoDocente(suspension.getSustituto().getIdDocente(), titular);
        List<Asignacion> asignaciones = asignacionRepository.findByDocenteIdDocenteAndAnioEscolarEstadoAndAccesoNot(
                titular.getIdDocente(), AnioEscolarService.ESTADO_VIGENTE, eliminado()).stream()
                .filter(a -> AccesoConstants.ACTIVO.equals(a.getAcceso().getIdAcceso()))
                .toList();
        if (asignaciones.isEmpty()) {
            cambio.setEstado(EstadoCambioDocente.CANCELADO);
            cambioRepository.save(cambio);
            return;
        }
        cambio.setAnioEscolar(asignaciones.get(0).getAnioEscolar());
        List<Integer> ids = asignaciones.stream().map(Asignacion::getIdAsignacion).sorted().toList();
        bloquear(ids, titular, sustituto);
        validarIncompatibilidad(ids, cambio.getIdCambio());
        List<CambioDocenteAsignacion> detalles = guardarDetalles(cambio, asignaciones, titular, sustituto);
        activar(cambio, automatica ? null : cambio.getUsuarioRegistro(),
                automatica ? OrigenOperacion.AUTOMATICO : OrigenOperacion.MANUAL, hoy, detalles);
    }

    Map<Integer, String> incidenciasDeCobertura(Collection<Integer> idsSuspension) {
        if (idsSuspension.isEmpty()) return Map.of();
        return cambioRepository.findBySuspensionIdSuspensionIn(idsSuspension).stream()
                .filter(c -> Boolean.TRUE.equals(c.getIncidencia()) && c.getMotivoIncidencia() != null)
                .collect(Collectors.toMap(c -> c.getSuspension().getIdSuspension(), CambioDocente::getMotivoIncidencia));
    }

    boolean cerrarCobertura(SuspensionDocente suspension, LocalDate hoy, boolean automatica) {
        Optional<CambioDocente> encontrada = cambioRepository.findBySuspensionIdSuspension(suspension.getIdSuspension());
        if (encontrada.isEmpty()) {
            return true;
        }
        CambioDocente cambio = encontrada.get();
        if (cambio.getEstado() == EstadoCambioDocente.PROGRAMADO) {
            cambio.setEstado(EstadoCambioDocente.CANCELADO);
            return true;
        }
        if (cambio.getEstado() != EstadoCambioDocente.VIGENTE) {
            return true;
        }
        Usuario usuario = automatica ? null : usuarioAutenticado();
        OrigenOperacion origen = automatica ? OrigenOperacion.AUTOMATICO : OrigenOperacion.MANUAL;
        return finalizarConDevolucion(cambio, usuario, origen, hoy, false);
    }

    private List<CambioDocenteAsignacion> guardarDetalles(CambioDocente cambio, List<Asignacion> asignaciones,
                                                           Docente titular, Docente sustituto) {
        List<CambioDocenteAsignacion> detalles = new ArrayList<>();
        for (Asignacion asignacion : asignaciones) {
            CambioDocenteAsignacion detalle = new CambioDocenteAsignacion();
            detalle.setCambio(cambio);
            detalle.setAsignacion(asignacion);
            detalle.setDocenteDesde(titular);
            detalle.setDocenteHacia(sustituto);
            detalle.setSinCobertura(sustituto == null);
            detalle.setDevuelta(false);
            detalle.setAcceso(activo());
            detalleRepository.save(detalle);
            detalles.add(detalle);
        }
        return detalles;
    }

    private void transferir(CambioDocente cambio, Usuario usuario, OrigenOperacion origen, LocalDate hoy) {
        transferir(cambio, usuario, origen, hoy, findDetalles(cambio.getIdCambio()));
    }

    private void transferir(CambioDocente cambio, Usuario usuario, OrigenOperacion origen, LocalDate hoy,
                            List<CambioDocenteAsignacion> detalles) {
        if (cambio.getDocenteNuevo() == null) {
            return;
        }
        validarAnioVigente(cambio.getAnioEscolar());
        Set<Docente> afectados = new LinkedHashSet<>();
        for (CambioDocenteAsignacion detalle : detalles) {
            if (Boolean.TRUE.equals(detalle.getDevuelta()) || detalle.getFechaTransferencia() != null) {
                continue;
            }
            Asignacion asignacion = detalle.getAsignacion();
            validarDocenteAsignacion(asignacion, detalle.getDocenteDesde());
            asignacion.setDocente(cambio.getDocenteNuevo());
            asignacionRepository.save(asignacion);
            detalle.setSinCobertura(false);
            detalle.setFechaTransferencia(hoy);
            detalle.setUsuarioTransferencia(usuario);
            detalle.setOrigenTransferencia(origen);
            detalleRepository.save(detalle);
            afectados.add(detalle.getDocenteDesde());
            afectados.add(cambio.getDocenteNuevo());
        }
        invalidarVinculos(afectados);
    }

    private boolean finalizarConDevolucion(CambioDocente cambio, Usuario usuario, OrigenOperacion origen,
                                           LocalDate hoy, boolean conservarIncidenciaPrevia) {
        List<CambioDocenteAsignacion> detalles = findDetalles(cambio.getIdCambio());
        bloquear(detalles.stream().map(d -> d.getAsignacion().getIdAsignacion()).sorted().toList(),
                cambio.getDocenteAnterior(), cambio.getDocenteNuevo());
        detalles = findDetalles(cambio.getIdCambio());

        List<CambioDocenteAsignacion> pendientes;
        try {
            pendientes = validarDevolucion(cambio, detalles);
        } catch (IllegalArgumentException ex) {
            if (!conservarIncidenciaPrevia || !Boolean.TRUE.equals(cambio.getIncidencia())
                    || cambio.getMotivoIncidencia() == null) {
                cambio.setMotivoIncidencia(ex.getMessage());
            }
            cambio.setIncidencia(true);
            cambio.setOrigenUltimaOperacion(origen);
            cambio.setUsuarioUltimaOperacion(usuario);
            cambioRepository.save(cambio);
            return false;
        }

        devolver(cambio, usuario, origen, hoy, pendientes);
        cambio.setEstado(EstadoCambioDocente.FINALIZADO);
        cambio.setFechaFinalizacion(hoy);
        cambio.setIncidencia(false);
        cambio.setMotivoIncidencia(null);
        cambio.setOrigenUltimaOperacion(origen);
        cambio.setUsuarioUltimaOperacion(usuario);
        cambioRepository.save(cambio);
        return true;
    }

    private List<CambioDocenteAsignacion> validarDevolucion(CambioDocente cambio,
                                                             List<CambioDocenteAsignacion> detalles) {
        validarAnioVigente(cambio.getAnioEscolar());
        List<CambioDocenteAsignacion> pendientes = detalles.stream()
                .filter(d -> !Boolean.TRUE.equals(d.getDevuelta()))
                .toList();
        if (!pendientes.isEmpty()) {
            List<Integer> ids = pendientes.stream()
                    .map(d -> d.getAsignacion().getIdAsignacion())
                    .sorted()
                    .toList();
            validarCompatibilidadHorarios(pendientes.get(0).getDocenteDesde(), cambio.getAnioEscolar(), ids);
            for (CambioDocenteAsignacion detalle : pendientes) {
                if (!Boolean.TRUE.equals(detalle.getSinCobertura())) {
                    validarDocenteAsignacion(detalle.getAsignacion(), detalle.getDocenteHacia());
                }
            }
        }
        return pendientes;
    }

    private void devolver(CambioDocente cambio, Usuario usuario, OrigenOperacion origen,
                          LocalDate hoy, List<CambioDocenteAsignacion> pendientes) {
        Set<Docente> afectados = new LinkedHashSet<>();
        for (CambioDocenteAsignacion detalle : pendientes) {
            if (!Boolean.TRUE.equals(detalle.getSinCobertura())) {
                Asignacion asignacion = detalle.getAsignacion();
                asignacion.setDocente(detalle.getDocenteDesde());
                asignacionRepository.save(asignacion);
            }
            detalle.setDevuelta(true);
            detalle.setFechaDevolucion(hoy);
            detalle.setUsuarioDevolucion(usuario);
            detalle.setOrigenDevolucion(origen);
            detalleRepository.save(detalle);
            afectados.add(detalle.getDocenteDesde());
            afectados.add(detalle.getDocenteHacia());
        }
        invalidarVinculos(afectados);
    }

    private void invalidarVinculos(Collection<Docente> docentes) {
        Set<Integer> ids = docentes.stream()
                .filter(Objects::nonNull)
                .map(Docente::getUsuario)
                .filter(Objects::nonNull)
                .map(Usuario::getIdUsuario)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        concesionesService.invalidarVinculoDespuesDeCommit(ids);
    }

    private MotivoCambioDocente validarMotivo(CambioDocenteRequest request) {
        MotivoCambioDocente motivo = validarMotivoYFechas(
                request.idMotivo(), request.tipo(), request.fechaInicio(), request.fechaFin());
        if (request.idDocenteSustituto() == null) {
            throw new IllegalArgumentException("El docente sustituto es obligatorio");
        }
        return motivo;
    }

    private MotivoCambioDocente validarMotivoYFechas(Integer idMotivo, TipoCambioDocente tipo,
                                                     LocalDate fechaInicio, LocalDate fechaFin) {
        MotivoCambioDocente motivo = motivoRepository
                .findByIdMotivoCambioDocenteAndAcceso(idMotivo, activo())
                .orElseThrow(() -> new IllegalArgumentException("El motivo de cambio no está activo"));
        boolean aplica = tipo == TipoCambioDocente.TEMPORAL
                ? Boolean.TRUE.equals(motivo.getAplicaTemporal())
                : Boolean.TRUE.equals(motivo.getAplicaDefinitivo());
        if (!aplica) {
            throw new IllegalArgumentException("El motivo no aplica para este tipo de cambio");
        }
        if (tipo == TipoCambioDocente.DEFINITIVO && fechaFin != null) {
            throw new IllegalArgumentException("Los cambios definitivos no admiten fecha de fin");
        }
        if (tipo == TipoCambioDocente.TEMPORAL && fechaFin != null
                && fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
        return motivo;
    }

    private List<Asignacion> validarAsignaciones(CambioDocenteRequest request) {
        Set<Integer> ids = new LinkedHashSet<>(request.idsAsignacion());
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos una asignación");
        }
        List<Integer> idsOrdenados = ids.stream().sorted().toList();
        Map<Integer, Asignacion> porId = asignacionRepository.findAllById(idsOrdenados).stream()
                .filter(a -> a.getAcceso() != null && AccesoConstants.ACTIVO.equals(a.getAcceso().getIdAcceso()))
                .collect(Collectors.toMap(Asignacion::getIdAsignacion, a -> a));
        List<Asignacion> asignaciones = idsOrdenados.stream()
                .map(id -> {
                    Asignacion asignacion = porId.get(id);
                    if (asignacion == null) {
                        throw new IllegalArgumentException("La asignación #" + id + " no está activa");
                    }
                    return asignacion;
                })
                .toList();
        Integer idAnio = asignaciones.get(0).getAnioEscolar().getIdAnio();
        Integer idTitular = asignaciones.get(0).getDocente().getIdDocente();
        for (Asignacion asignacion : asignaciones) {
            if (!asignacion.getAnioEscolar().getIdAnio().equals(idAnio)
                    || !asignacion.getDocente().getIdDocente().equals(idTitular)) {
                throw new IllegalArgumentException("Todas las asignaciones deben ser del mismo año y docente");
            }
            validarAnioVigente(asignacion.getAnioEscolar());
        }
        return asignaciones;
    }

    private Docente validarSustituto(CambioDocenteRequest request, Docente titular) {
        return validarSustitutoDocente(request.idDocenteSustituto(), titular);
    }

    private Docente validarSustitutoDocente(Integer idDocente, Docente titular) {
        if (idDocente == null) {
            throw new IllegalArgumentException("El docente sustituto es obligatorio");
        }
        Docente sustituto = findDocente(idDocente);
        if (!sustituto.getAcceso().getIdAcceso().equals(AccesoConstants.ACTIVO)) {
            throw new IllegalArgumentException("El sustituto debe estar activo");
        }
        if (sustituto.getIdDocente().equals(titular.getIdDocente())) {
            throw new IllegalArgumentException("El titular no puede ser su propio sustituto");
        }
        if (suspensionRepository.existsByDocenteAndEstadoInAndAccesoNot(
                sustituto, List.of(EstadoSuspension.PROGRAMADA, EstadoSuspension.VIGENTE), eliminado())) {
            throw new IllegalArgumentException("El sustituto tiene una suspensión laboral vigente");
        }
        return sustituto;
    }

    private void validarCompatibilidadHorarios(Docente receptor, AnioEscolar anio,
                                                Collection<Integer> idsAsignacion) {
        if (receptor == null) {
            throw new IllegalArgumentException("El docente receptor no está disponible");
        }
        List<HorarioClase> horariosReceptor = horarioClaseRepository
                .findByAsignacionDocenteIdDocenteAndAsignacionAnioEscolarIdAnioAndAccesoNot(
                        receptor.getIdDocente(), anio.getIdAnio(), eliminado()).stream()
                .filter(h -> !idsAsignacion.contains(h.getAsignacion().getIdAsignacion()))
                .filter(h -> AccesoConstants.ACTIVO.equals(h.getAcceso().getIdAcceso()))
                .filter(h -> AccesoConstants.ACTIVO.equals(h.getAsignacion().getAcceso().getIdAcceso()))
                .toList();
        List<HorarioClase> horariosAsignaciones = horarioClaseRepository
                .findByAsignacionIdAsignacionInAndAccesoNot(idsAsignacion, eliminado()).stream()
                .filter(h -> AccesoConstants.ACTIVO.equals(h.getAcceso().getIdAcceso()))
                .toList();
        validarCrucesAsignaciones(horariosAsignaciones);
        for (HorarioClase horario : horariosAsignaciones) {
            for (HorarioClase otro : horariosReceptor) {
                if (horario.getDiaSemana() == otro.getDiaSemana()
                        && HorarioUtil.seCruzan(horario.getHoraInicio(), horario.getHoraFin(),
                        otro.getHoraInicio(), otro.getHoraFin())) {
                    throw new IllegalArgumentException("El docente receptor ya tiene clase el "
                            + HorarioUtil.dia(horario.getDiaSemana()) + " de "
                            + HorarioUtil.rango(horario.getHoraInicio(), horario.getHoraFin()));
                }
            }
        }
    }

    private void validarCrucesAsignaciones(List<HorarioClase> horarios) {
        for (int i = 0; i < horarios.size(); i++) {
            HorarioClase horario = horarios.get(i);
            for (int j = i + 1; j < horarios.size(); j++) {
                HorarioClase otro = horarios.get(j);
                if (horario.getAsignacion().getIdAsignacion().equals(otro.getAsignacion().getIdAsignacion())) {
                    continue;
                }
                if (horario.getDiaSemana() == otro.getDiaSemana()
                        && HorarioUtil.seCruzan(horario.getHoraInicio(), horario.getHoraFin(),
                        otro.getHoraInicio(), otro.getHoraFin())) {
                    throw new IllegalArgumentException("Las asignaciones seleccionadas tienen un cruce de horario el "
                            + HorarioUtil.dia(horario.getDiaSemana()) + " de "
                            + HorarioUtil.rango(horario.getHoraInicio(), horario.getHoraFin()));
                }
            }
        }
    }

    private void validarIncompatibilidad(List<Integer> ids, Integer idCambioExcluido) {
        Set<Integer> ocupadas = detalleRepository
                .findByAsignacionIdAsignacionInAndDevueltaFalseAndAccesoNot(ids, eliminado()).stream()
                .filter(d -> d.getCambio() != null
                        && !Objects.equals(idCambioExcluido, d.getCambio().getIdCambio())
                        && EstadoCambioDocente.ACTIVOS.contains(d.getCambio().getEstado()))
                .map(d -> d.getAsignacion().getIdAsignacion())
                .collect(Collectors.toSet());
        for (Integer idAsignacion : ids) {
            if (ocupadas.contains(idAsignacion)) {
                throw new IllegalArgumentException("La asignación #" + idAsignacion
                        + " ya participa en un cambio de docente pendiente");
            }
        }
    }

    private void validarDocenteAsignacion(Asignacion asignacion, Docente esperado) {
        if (esperado == null || !esperado.getIdDocente().equals(asignacion.getDocente().getIdDocente())) {
            throw new IllegalArgumentException("La asignación #" + asignacion.getIdAsignacion()
                    + " no está en el docente esperado; requiere resolución administrativa");
        }
    }

    private void bloquear(List<Integer> ids, Docente titular, Docente sustituto) {
        ids.forEach(bloqueo::asignacion);
        bloqueo.docente(titular.getIdDocente());
        if (sustituto != null) {
            bloqueo.docente(sustituto.getIdDocente());
        }
    }

    private EstadoCambioDocente estadoInicial(CambioDocenteRequest request, LocalDate hoy) {
        if (request.fechaInicio().isAfter(hoy)) {
            return EstadoCambioDocente.PROGRAMADO;
        }
        return request.tipo() == TipoCambioDocente.DEFINITIVO
                ? EstadoCambioDocente.APLICADO
                : EstadoCambioDocente.VIGENTE;
    }

    private List<CambioDocenteAsignacion> findDetalles(Integer idCambio) {
        return detalleRepository.findByCambioIdCambioAndAccesoNot(idCambio, eliminado()).stream()
                .sorted(Comparator.comparing(d -> d.getAsignacion().getIdAsignacion()))
                .toList();
    }

    private void validarAnioVigente(AnioEscolar anio) {
        if (anio == null || anio.getEstado() != AnioEscolarService.ESTADO_VIGENTE) {
            throw new IllegalArgumentException(
                    "El año escolar ya no está vigente; la operación requiere resolución administrativa");
        }
    }

    private CambioDocente buscar(Integer id) {
        CambioDocente cambio = cambioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cambio de docente no encontrado con id " + id));
        validarCambioVisible(cambio, id);
        return cambio;
    }

    private CambioDocente buscarBloqueado(Integer id) {
        CambioDocente cambio = cambioRepository.bloquear(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cambio de docente no encontrado con id " + id));
        validarCambioVisible(cambio, id);
        return cambio;
    }

    private void validarCambioVisible(CambioDocente cambio, Integer id) {
        if (cambio.getSuspension() != null || cambio.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO)) {
            throw new ResourceNotFoundException("Cambio de docente no encontrado con id " + id);
        }
    }

    private Docente findDocente(Integer id) {
        return docenteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Docente no encontrado con id " + id));
    }

    private Usuario usuarioAutenticado() {
        Integer idUsuario = contexto.idUsuarioAutenticado();
        if (idUsuario == null) {
            throw new IllegalArgumentException("Se requiere usuario autenticado para registrar el cambio");
        }
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + idUsuario));
    }

    private Acceso activo() {
        return accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow();
    }

    private Acceso eliminado() {
        return accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
    }

    private List<CambioDocenteResponse> toResponses(List<CambioDocente> cambios) {
        if (cambios.isEmpty()) {
            return List.of();
        }
        List<Integer> idsCambio = cambios.stream()
                .map(CambioDocente::getIdCambio)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<CambioDocenteAsignacion> detalles = idsCambio.isEmpty()
                ? List.of()
                : detalleRepository.findByCambioIdCambioInAndAccesoNot(idsCambio, eliminado());
        Map<Integer, List<CambioDocenteAsignacion>> detallesPorCambio = new LinkedHashMap<>();
        for (CambioDocenteAsignacion detalle : detalles) {
            detallesPorCambio.computeIfAbsent(detalle.getCambio().getIdCambio(), k -> new ArrayList<>()).add(detalle);
        }
        detallesPorCambio.values().forEach(list -> list.sort(
                Comparator.comparing(d -> d.getAsignacion().getIdAsignacion())));
        Map<Integer, List<CambioDocenteResponse.HorarioResumen>> horariosPorAsignacion =
                horariosPorAsignacion(detalles);
        return cambios.stream()
                .map(cambio -> toResponse(cambio, detallesPorCambio, horariosPorAsignacion))
                .toList();
    }

    private CambioDocenteResponse toResponse(CambioDocente cambio) {
        List<CambioDocenteAsignacion> detalles = findDetalles(cambio.getIdCambio());
        return toResponse(cambio, Map.of(cambio.getIdCambio(), detalles), horariosPorAsignacion(detalles));
    }

    private Map<Integer, List<CambioDocenteResponse.HorarioResumen>> horariosPorAsignacion(
            List<CambioDocenteAsignacion> detalles) {
        Map<Integer, List<CambioDocenteResponse.HorarioResumen>> horariosPorAsignacion = new LinkedHashMap<>();
        List<Integer> idsAsignacion = detalles.stream()
                .map(d -> d.getAsignacion().getIdAsignacion())
                .distinct()
                .toList();
        if (!idsAsignacion.isEmpty()) {
            for (HorarioClase horario : horarioClaseRepository
                    .findByAsignacionIdAsignacionInAndAccesoNot(idsAsignacion, eliminado())) {
                if (!AccesoConstants.ACTIVO.equals(horario.getAcceso().getIdAcceso())
                        || horario.getAsignacion() == null) {
                    continue;
                }
                Integer idAsignacion = horario.getAsignacion().getIdAsignacion();
                horariosPorAsignacion.computeIfAbsent(idAsignacion, k -> new ArrayList<>())
                        .add(new CambioDocenteResponse.HorarioResumen(
                                horario.getIdHorario(),
                                Integer.valueOf(horario.getDiaSemana()),
                                horario.getHoraInicio() == null ? null : horario.getHoraInicio().toString(),
                                horario.getHoraFin() == null ? null : horario.getHoraFin().toString()));
            }
            horariosPorAsignacion.values().forEach(list -> list.sort(
                    Comparator.comparing(CambioDocenteResponse.HorarioResumen::diaSemana)
                            .thenComparing(CambioDocenteResponse.HorarioResumen::horaInicio)));
        }
        return horariosPorAsignacion;
    }

    private CambioDocenteResponse toResponse(CambioDocente cambio,
                                             Map<Integer, List<CambioDocenteAsignacion>> detallesPorCambio,
                                             Map<Integer, List<CambioDocenteResponse.HorarioResumen>> horariosPorAsignacion) {
        CambioDocenteResponse response = new CambioDocenteResponse();
        response.setIdCambio(cambio.getIdCambio());
        response.setTipo(cambio.getTipo());
        response.setEstado(cambio.getEstado());
        response.setIdAnio(cambio.getAnioEscolar() == null ? null : cambio.getAnioEscolar().getIdAnio());
        response.setAnio(cambio.getAnioEscolar() == null ? null : cambio.getAnioEscolar().getAnio());
        response.setFechaCambio(cambio.getFechaCambio());
        response.setFechaInicio(cambio.getFechaInicio());
        response.setFechaFin(cambio.getFechaFin());
        response.setFechaAplicacion(cambio.getFechaAplicacion());
        response.setFechaFinalizacion(cambio.getFechaFinalizacion());
        response.setIdMotivo(cambio.getMotivoRelacionado() == null ? null : cambio.getMotivoRelacionado().getIdMotivoCambioDocente());
        response.setMotivo(cambio.getMotivo());
        response.setMotivoDetalle(cambio.getMotivoDetalle());
        if (cambio.getDocenteAnterior() != null) {
            response.setIdDocenteAnterior(cambio.getDocenteAnterior().getIdDocente());
            response.setDocenteAnterior(nombre(cambio.getDocenteAnterior()));
        }
        if (cambio.getDocenteNuevo() != null) {
            response.setIdDocenteNuevo(cambio.getDocenteNuevo().getIdDocente());
            response.setDocenteNuevo(nombre(cambio.getDocenteNuevo()));
        }
        if (cambio.getUsuarioRegistro() != null) {
            response.setIdUsuarioRegistro(cambio.getUsuarioRegistro().getIdUsuario());
            response.setUsuarioRegistro(cambio.getUsuarioRegistro().getNombre() + " "
                    + cambio.getUsuarioRegistro().getApellidoPat());
        }
        response.setOrigenRegistro(cambio.getOrigenRegistro());
        response.setOrigenUltimaOperacion(cambio.getOrigenUltimaOperacion());
        response.setIdUsuarioUltimaOperacion(cambio.getUsuarioUltimaOperacion() == null
                ? null : cambio.getUsuarioUltimaOperacion().getIdUsuario());
        response.setIncidencia(cambio.getIncidencia());
        response.setMotivoIncidencia(cambio.getMotivoIncidencia());
        response.setResolucionAdministrativa(cambio.getResolucionAdministrativa());
        response.setFechaResolucion(cambio.getFechaResolucion());
        response.setMotivoResolucion(cambio.getMotivoResolucion());
        response.setAccesoId(cambio.getAcceso().getIdAcceso().longValue());
        List<CambioDocenteAsignacion> detallesCambio =
                detallesPorCambio.getOrDefault(cambio.getIdCambio(), List.of());
        List<CambioDocenteResponse.Detalle> detalles = new ArrayList<>();
        for (CambioDocenteAsignacion detalle : detallesCambio) {
            Asignacion asignacion = detalle.getAsignacion();
            detalles.add(new CambioDocenteResponse.Detalle(
                    asignacion.getIdAsignacion(),
                    detalle.getDocenteDesde().getIdDocente(),
                    detalle.getDocenteHacia() == null ? null : detalle.getDocenteHacia().getIdDocente(),
                    detalle.getSinCobertura(),
                    detalle.getDevuelta(),
                    detalle.getFechaTransferencia(),
                    detalle.getFechaDevolucion(),
                    detalle.getOrigenTransferencia(),
                    detalle.getOrigenDevolucion(),
                    asignacion.getCurso() == null ? null : asignacion.getCurso().getNombre(),
                    asignacion.getGradoSeccion() == null || asignacion.getGradoSeccion().getGrado() == null
                            ? null : asignacion.getGradoSeccion().getGrado().getNombre(),
                    asignacion.getGradoSeccion() == null || asignacion.getGradoSeccion().getSeccion() == null
                            ? null : asignacion.getGradoSeccion().getSeccion().getNombre(),
                    asignacion.getAnioEscolar() == null ? null : asignacion.getAnioEscolar().getAnio(),
                    horariosPorAsignacion.getOrDefault(asignacion.getIdAsignacion(), List.of())
            ));
        }
        response.setDetalles(detalles);
        return response;
    }

    private String nombre(Docente docente) {
        return docente.getUsuario().getNombre() + " " + docente.getUsuario().getApellidoPat()
                + " " + docente.getUsuario().getApellidoMat();
    }
}
