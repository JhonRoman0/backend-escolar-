package com.example.Escolar.Service;

import com.example.Escolar.Dto.*;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.*;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Exception.AccesoDenegadoException;
import com.example.Escolar.Security.ActorSeguridad;
import com.example.Escolar.Security.ConcesionesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolPermisoService {

    private final RolPermisoRepository rolPermisoRepository;
    private final AccesoRepository accesoRepository;
    private final RolPermisoAccionRepository rolPermisoAccionRepository;
    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final AccionRepository accionRepository;
    private final PermisoAccionRepository permisoAccionRepository;
    private final ConcesionesService concesionesService;
    private final ActorSeguridad actorSeguridad;

    public List<RolPermisoResponse> getAll() {
        return rolPermisoRepository.findByAccesoNot(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream().map(this::toResponse).toList();
    }

    public RolPermisoResponse getById(Integer id) {
        return toResponse(findRolPermiso(id));
    }

    @Transactional
    public RolPermisoResponse create(RolPermisoRequest request) {
        if (request.getIdRol() == null || request.getIdPermiso() == null) {
            throw new IllegalArgumentException("El rol y el permiso son obligatorios");
        }
        Rol rol = findRol(request.getIdRol());
        Permiso permiso = findPermiso(request.getIdPermiso());
        List<String> acciones = request.getAcciones() == null ? List.of() : request.getAcciones();
        validarCambio(rol, permiso, List.of(), acciones);
        Acceso acceso = accesoParaAsignacion(request.getAccesoId());
        Optional<RolPermiso> existente = rolPermisoRepository.findByRolIdRolAndPermisoIdPermiso(rol.getIdRol(), permiso.getIdPermiso());
        if (existente.isPresent()) {
            RolPermiso rolPermiso = existente.get();
            if (!rolPermiso.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO)) {
                throw new IllegalArgumentException("El permiso ya está asignado a este rol");
            }
            rolPermiso.setAcceso(acceso);
            rolPermiso = rolPermisoRepository.save(rolPermiso);
            reemplazarAccionesConcedidas(rolPermiso, acciones);
            concesionesService.invalidarPermisosDelRol(rol.getIdRol());
            return toResponse(rolPermiso);
        }
        RolPermiso rolPermiso = new RolPermiso();
        rolPermiso.setRol(rol);
        rolPermiso.setPermiso(permiso);
        rolPermiso.setFechaAsignacion(LocalDateTime.now());
        rolPermiso.setAcceso(acceso);
        rolPermiso = rolPermisoRepository.save(rolPermiso);
        asignarAccionesConcedidas(rolPermiso, acciones);
        concesionesService.invalidarPermisosDelRol(rol.getIdRol());
        return toResponse(rolPermiso);
    }

    /** Editar una asignación solo cambia sus acciones (o su estado activo/inactivo). */
    @Transactional
    public RolPermisoResponse update(Integer id, RolPermisoRequest request) {
        RolPermiso rolPermiso = findRolPermiso(id);
        if (request.getIdRol() != null && !request.getIdRol().equals(rolPermiso.getRol().getIdRol())) {
            throw new IllegalArgumentException("Una asignación no se puede pasar a otro rol");
        }
        if (request.getIdPermiso() != null && !request.getIdPermiso().equals(rolPermiso.getPermiso().getIdPermiso())) {
            throw new IllegalArgumentException("Una asignación no se puede cambiar a otro permiso");
        }
        List<String> antes = accionesConcedidasDeRolPermiso(rolPermiso.getIdRolPermiso());
        List<String> despues = request.getAcciones() == null ? antes : request.getAcciones();
        validarCambio(rolPermiso.getRol(), rolPermiso.getPermiso(), antes, despues);
        if (request.getAccesoId() != null) {
            rolPermiso.setAcceso(accesoParaAsignacion(request.getAccesoId()));
        }
        rolPermiso = rolPermisoRepository.save(rolPermiso);
        if (request.getAcciones() != null) {
            reemplazarAccionesConcedidas(rolPermiso, request.getAcciones());
        }
        concesionesService.invalidarPermisosDelRol(rolPermiso.getRol().getIdRol());
        return toResponse(rolPermiso);
    }

    @Transactional
    public void delete(Integer id) {
        RolPermiso rolPermiso = findRolPermiso(id);
        Rol rol = rolPermiso.getRol();
        validarCambio(rol, rolPermiso.getPermiso(), accionesConcedidasDeRolPermiso(rolPermiso.getIdRolPermiso()), List.of());
        rolPermiso.setAcceso(accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow());
        rolPermisoRepository.save(rolPermiso);
        if (rol != null) concesionesService.invalidarPermisosDelRol(rol.getIdRol());
    }

    /**
     * Reglas para no darse más poder (el ADMIN no las tiene):
     * - el rol ADMIN no se configura: ya tiene todo;
     * - nadie cambia los permisos de cualquiera de sus roles asignados;
     * - solo se dan o quitan acciones que uno mismo tiene en ese permiso.
     */
    private void validarCambio(Rol rol, Permiso permiso, List<String> antes, List<String> despues) {
        if (RolConstants.esAdmin(rol.getNombre())) {
            throw new IllegalArgumentException("El rol ADMIN tiene acceso a todo: sus permisos no se configuran");
        }
        if (actorSeguridad.esAdminOSistema()) {
            return;
        }
        if (actorSeguridad.idsRolesActuales().contains(rol.getIdRol())) {
            throw new AccesoDenegadoException("No puedes modificar los permisos de tu propio rol");
        }
        Integer idActor = actorSeguridad.idActual().orElseThrow();
        Integer idRolActor = actorSeguridad.idRolActual().orElseThrow();
        Set<String> propias = concesionesService.accionesPorPermiso(idActor, idRolActor).get(permiso.getCodigo());
        if (propias == null) {
            throw new AccesoDenegadoException("No puedes asignar ni quitar el permiso " + permiso.getNombre()
                    + " porque tú no lo tienes");
        }
        Set<String> cambiadas = new HashSet<>(antes);
        cambiadas.addAll(despues);
        Set<String> comunes = new HashSet<>(antes);
        comunes.retainAll(despues);
        cambiadas.removeAll(comunes);
        cambiadas.removeAll(propias);
        if (!cambiadas.isEmpty()) {
            throw new AccesoDenegadoException("Solo puedes dar o quitar acciones que tú tienes en "
                    + permiso.getNombre() + ": " + String.join(", ", new java.util.TreeSet<>(cambiadas)));
        }
    }

    /** Una asignación solo queda activa o inactiva por aquí; quitarla es con DELETE. */
    private Acceso accesoParaAsignacion(Long accesoId) {
        if (accesoId == null) {
            return accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow();
        }
        if (!AccesoConstants.ACTIVO.equals(accesoId) && !AccesoConstants.INACTIVO.equals(accesoId)) {
            throw new IllegalArgumentException("Estado no válido: para quitar un permiso usa la opción Quitar");
        }
        return accesoRepository.findById(accesoId).orElseThrow();
    }

    public PermisosRolResponse obtenerPermisosDelRol(Integer idRol) {
        Rol rol = findRol(idRol);
        List<RolPermiso> asignaciones = rolPermisoRepository.findByRolIdRol(idRol).stream()
                .filter(rp -> !rp.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                .filter(rp -> !rp.getPermiso().getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                .filter(rp -> !rp.getPermiso().getModulo().getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                .toList();

        PermisosRolResponse response = new PermisosRolResponse();
        response.setIdRol(rol.getIdRol());
        response.setNombreRol(rol.getNombre());
        response.setModulos(new ArrayList<>());

        asignaciones.stream()
                .map(rp -> rp.getPermiso().getModulo())
                .distinct()
                .sorted(Comparator.comparing(Modulo::getIdModulo))
                .forEach(modulo -> {
                    ModuloPermisosResponse moduloResponse = new ModuloPermisosResponse();
                    moduloResponse.setIdModulo(modulo.getIdModulo());
                    moduloResponse.setModulo(modulo.getModulo());
                    moduloResponse.setIcono(modulo.getIcono());

                    List<PermisoAccionResponse> permisos = asignaciones.stream()
                            .filter(rp -> rp.getPermiso().getModulo().getIdModulo().equals(modulo.getIdModulo()))
                            .map(this::toPermisoAccionResponse)
                            .sorted(Comparator.comparing(p -> p.getNombre() == null ? "" : p.getNombre()))
                            .toList();
                    moduloResponse.setPermisos(permisos);
                    response.getModulos().add(moduloResponse);
                });

        return response;
    }

    public PermisosRolResponse obtenerPermisosDelUsuario(Integer idUsuario, Integer idRolActivo) {
        PermisosRolResponse response = new PermisosRolResponse();
        response.setModulos(new ArrayList<>());

        if (!concesionesService.rolActivoValido(idUsuario, idRolActivo)) {
            return response;
        }
        response.setIdRol(idRolActivo);
        response.setNombreRol(concesionesService.nombreRolActivo(idUsuario, idRolActivo));
        List<RolPermiso> rolPermisos = rolPermisoRepository.findByRolIdRol(idRolActivo).stream()
                .filter(rp -> rp.getAcceso().getIdAcceso().equals(AccesoConstants.ACTIVO))
                .filter(rp -> !rp.getPermiso().getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                .filter(rp -> !rp.getPermiso().getModulo().getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                .toList();
        if (rolPermisos.isEmpty()) {
            return response;
        }

        Map<Integer, Set<String>> concedidasPorPermiso = new HashMap<>();
        Map<Integer, PermisoAccionResponse> permisoPorId = new LinkedHashMap<>();
        Map<Integer, ModuloPermisosResponse> modulosPorId = new LinkedHashMap<>();

        for (RolPermiso rolPermiso : rolPermisos) {
            Permiso permiso = rolPermiso.getPermiso();
            Modulo modulo = permiso.getModulo();

            PermisoAccionResponse permisoResponse = permisoPorId.computeIfAbsent(permiso.getIdPermiso(), id -> {
                PermisoAccionResponse pr = new PermisoAccionResponse();
                pr.setIdPermiso(permiso.getIdPermiso());
                pr.setCodigo(permiso.getCodigo());
                pr.setNombre(permiso.getNombre());
                pr.setAccionesDisponibles(accionesDisponiblesDelPermiso(permiso.getIdPermiso()));
                pr.setAccionesConcedidas(new ArrayList<>());
                return pr;
            });

            ModuloPermisosResponse moduloResponse = modulosPorId.computeIfAbsent(modulo.getIdModulo(), id -> {
                ModuloPermisosResponse mr = new ModuloPermisosResponse();
                mr.setIdModulo(modulo.getIdModulo());
                mr.setModulo(modulo.getModulo());
                mr.setIcono(modulo.getIcono());
                mr.setPermisos(new ArrayList<>());
                return mr;
            });
            if (!moduloResponse.getPermisos().contains(permisoResponse)) {
                moduloResponse.getPermisos().add(permisoResponse);
            }

            Set<String> concedidas = concedidasPorPermiso.computeIfAbsent(permiso.getIdPermiso(), id -> new HashSet<>());
            for (RolPermisoAccion rolPermisoAccion : rolPermisoAccionRepository
                    .findByRolPermisoIdRolPermiso(rolPermiso.getIdRolPermiso())) {
                if (rolPermisoAccion.getAcceso().getIdAcceso().equals(AccesoConstants.ACTIVO)
                        && !rolPermisoAccion.getAccion().getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO)) {
                    concedidas.add(rolPermisoAccion.getAccion().getCodigo());
                }
            }
        }

        concedidasPorPermiso.forEach((idPermiso, acciones) ->
                permisoPorId.get(idPermiso).setAccionesConcedidas(acciones.stream().sorted().toList()));

        response.setModulos(modulosPorId.values().stream()
                .sorted(Comparator.comparing(ModuloPermisosResponse::getIdModulo))
                .toList());
        return response;
    }

    private void asignarAccionesConcedidas(RolPermiso rolPermiso, List<String> codigosAcciones) {
        if (codigosAcciones == null || codigosAcciones.isEmpty()) {
            return;
        }
        validarAccionesSoportadas(rolPermiso, codigosAcciones);
        for (String codigo : codigosAcciones) {
            Accion accion = findAccion(codigo);
            if (rolPermisoAccionRepository.findByRolPermisoIdRolPermisoAndAccionIdAccionAndAccesoNot(rolPermiso.getIdRolPermiso(), accion.getIdAccion(), accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).isEmpty()) {
                crearAccionConcedida(rolPermiso, accion);
            }
        }
    }

    private void reemplazarAccionesConcedidas(RolPermiso rolPermiso, List<String> codigosAcciones) {
        rolPermisoAccionRepository.deleteAll(
                rolPermisoAccionRepository.findByRolPermisoIdRolPermiso(rolPermiso.getIdRolPermiso()));
        asignarAccionesConcedidas(rolPermiso, codigosAcciones);
    }

    private void validarAccionesSoportadas(RolPermiso rolPermiso, List<String> codigosAcciones) {
        Set<String> soportadas = accionesSoportadas(rolPermiso.getPermiso().getIdPermiso());
        for (String codigo : codigosAcciones) {
            if (!soportadas.contains(codigo)) {
                throw new IllegalArgumentException("El permiso " + rolPermiso.getPermiso().getCodigo()
                        + " no permite la acción " + codigo);
            }
        }
    }

    private Set<String> accionesSoportadas(Integer idPermiso) {
        return permisoAccionRepository.findByPermisoIdPermisoAndAccesoNot(idPermiso, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .filter(pa -> !pa.getAccion().getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                .map(pa -> pa.getAccion().getCodigo())
                .collect(Collectors.toSet());
    }

    private void crearAccionConcedida(RolPermiso rolPermiso, Accion accion) {
        RolPermisoAccion rolPermisoAccion = new RolPermisoAccion();
        rolPermisoAccion.setRolPermiso(rolPermiso);
        rolPermisoAccion.setAccion(accion);
        rolPermisoAccion.setAcceso(accesoRepository.findById(AccesoConstants.ACTIVO).orElseThrow());
        rolPermisoAccionRepository.save(rolPermisoAccion);
    }

    private Accion findAccion(String codigo) {
        return accionRepository.findByCodigoAndAccesoNot(codigo, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new IllegalArgumentException("La acción " + codigo + " no existe"));
    }

    private Rol findRol(Integer id) {
        return rolRepository.findByIdRolAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id " + id));
    }

    private Permiso findPermiso(Integer id) {
        return permisoRepository.findByIdPermisoAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("Permiso no encontrado con id " + id));
    }

    private RolPermiso findRolPermiso(Integer id) {
        return rolPermisoRepository.findByIdRolPermisoAndAccesoNot(id, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow())
                .orElseThrow(() -> new ResourceNotFoundException("RolPermiso no encontrado con id " + id));
    }

    private List<String> accionesDisponiblesDelPermiso(Integer idPermiso) {
        return permisoAccionRepository.findByPermisoIdPermisoAndAccesoNot(idPermiso, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .filter(pa -> !pa.getAccion().getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                .map(pa -> pa.getAccion().getCodigo())
                .sorted()
                .toList();
    }

    private List<String> accionesConcedidasDeRolPermiso(Integer idRolPermiso) {
        return rolPermisoAccionRepository.findByRolPermisoIdRolPermisoAndAccesoNot(idRolPermiso, accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow()).stream()
                .filter(ra -> !ra.getAccion().getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO))
                .map(ra -> ra.getAccion().getCodigo())
                .sorted()
                .toList();
    }

    private PermisoAccionResponse toPermisoAccionResponse(RolPermiso rolPermiso) {
        PermisoAccionResponse response = new PermisoAccionResponse();
        response.setIdPermiso(rolPermiso.getPermiso().getIdPermiso());
        response.setCodigo(rolPermiso.getPermiso().getCodigo());
        response.setNombre(rolPermiso.getPermiso().getNombre());
        response.setAccionesDisponibles(accionesDisponiblesDelPermiso(rolPermiso.getPermiso().getIdPermiso()));
        response.setAccionesConcedidas(accionesConcedidasDeRolPermiso(rolPermiso.getIdRolPermiso()));
        return response;
    }

    private RolPermisoResponse toResponse(RolPermiso rolPermiso) {
        RolPermisoResponse response = new RolPermisoResponse();
        response.setIdRolPermiso(rolPermiso.getIdRolPermiso());
        response.setRol(toRolResponse(rolPermiso.getRol()));
        response.setPermiso(toPermisoResponse(rolPermiso.getPermiso()));
        response.setAcciones(accionesConcedidasDeRolPermiso(rolPermiso.getIdRolPermiso()));
        response.setAccesoId(rolPermiso.getAcceso().getIdAcceso().longValue());
        return response;
    }

    private RolResponse toRolResponse(Rol rol) {
        RolResponse response = new RolResponse();
        response.setIdRol(rol.getIdRol());
        response.setNombre(rol.getNombre());
        response.setAccesoId(rol.getAcceso().getIdAcceso().longValue());
        return response;
    }

    private PermisoResponse toPermisoResponse(Permiso permiso) {
        PermisoResponse response = new PermisoResponse();
        response.setIdPermiso(permiso.getIdPermiso());
        response.setCodigo(permiso.getCodigo());
        response.setNombre(permiso.getNombre());
        response.setAccesoId(permiso.getAcceso().getIdAcceso().longValue());
        response.setAcciones(accionesDisponiblesDelPermiso(permiso.getIdPermiso()));
        return response;
    }
}
