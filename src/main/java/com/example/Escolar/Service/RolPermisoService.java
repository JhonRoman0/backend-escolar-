package com.example.Escolar.Service;

import com.example.Escolar.Dto.*;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolPermisoService {

    public static final byte ACCESO_ACTIVO = 1;
    public static final byte ACCESO_ELIMINADO = 2;

    private final RolPermisoRepository rolPermisoRepository;
    private final RolPermisoAccionRepository rolPermisoAccionRepository;
    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final AccionRepository accionRepository;
    private final PermisoAccionRepository permisoAccionRepository;
    private final UsuarioRolRepository usuarioRolRepository;

    public List<RolPermisoResponse> getAll() {
        return rolPermisoRepository.findByAccesoNot(ACCESO_ELIMINADO).stream().map(this::toResponse).toList();
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
        Optional<RolPermiso> existente = rolPermisoRepository.findByRolIdRolAndPermisoIdPermiso(rol.getIdRol(), permiso.getIdPermiso());
        if (existente.isPresent()) {
            RolPermiso rolPermiso = existente.get();
            if (rolPermiso.getAcceso() != ACCESO_ELIMINADO) {
                throw new IllegalArgumentException("El permiso ya está asignado a este rol");
            }
            rolPermiso.setAcceso(request.getAcceso() != null ? request.getAcceso() : ACCESO_ACTIVO);
            rolPermiso = rolPermisoRepository.save(rolPermiso);
            if (request.getAcciones() != null) {
                reemplazarAccionesConcedidas(rolPermiso, request.getAcciones());
            }
            return toResponse(rolPermiso);
        }
        RolPermiso rolPermiso = new RolPermiso();
        rolPermiso.setRol(rol);
        rolPermiso.setPermiso(permiso);
        rolPermiso.setFechaAsignacion(LocalDateTime.now());
        rolPermiso.setAcceso(request.getAcceso() != null ? request.getAcceso() : ACCESO_ACTIVO);
        rolPermiso = rolPermisoRepository.save(rolPermiso);
        asignarAccionesConcedidas(rolPermiso, request.getAcciones());
        return toResponse(rolPermiso);
    }

    @Transactional
    public RolPermisoResponse update(Integer id, RolPermisoRequest request) {
        RolPermiso rolPermiso = findRolPermiso(id);
        if (request.getIdRol() != null) {
            rolPermiso.setRol(findRol(request.getIdRol()));
        }
        if (request.getIdPermiso() != null) {
            Permiso nuevoPermiso = findPermiso(request.getIdPermiso());
            RolPermiso existente = rolPermisoRepository
                    .findByRolIdRolAndPermisoIdPermiso(rolPermiso.getRol().getIdRol(), nuevoPermiso.getIdPermiso())
                    .orElse(null);
            if (existente != null && !existente.getIdRolPermiso().equals(rolPermiso.getIdRolPermiso())) {
                if (existente.getAcceso() == ACCESO_ELIMINADO) {
                    existente.setAcceso(request.getAcceso() != null ? request.getAcceso() : ACCESO_ACTIVO);
                    reemplazarAccionesConcedidas(existente, accionesConcedidasDeRolPermiso(rolPermiso.getIdRolPermiso()));
                    rolPermisoRepository.save(existente);
                    rolPermiso.setAcceso(ACCESO_ELIMINADO);
                    rolPermisoRepository.save(rolPermiso);
                    return toResponse(existente);
                }
                throw new IllegalArgumentException("El permiso ya está asignado a este rol");
            }
            rolPermiso.setPermiso(nuevoPermiso);
            revalidarAccionesContraPermiso(rolPermiso);
        }
        if (request.getAcceso() != null) {
            rolPermiso.setAcceso(request.getAcceso());
        }
        rolPermiso = rolPermisoRepository.save(rolPermiso);
        if (request.getAcciones() != null) {
            reemplazarAccionesConcedidas(rolPermiso, request.getAcciones());
        }
        return toResponse(rolPermiso);
    }

    @Transactional
    public void delete(Integer id) {
        RolPermiso rolPermiso = findRolPermiso(id);
        rolPermiso.setAcceso(ACCESO_ELIMINADO);
        rolPermisoRepository.save(rolPermiso);
    }

    public PermisosRolResponse obtenerPermisosDelRol(Integer idRol) {
        Rol rol = findRol(idRol);
        List<RolPermiso> asignaciones = rolPermisoRepository.findByRolIdRol(idRol).stream()
                .filter(rp -> rp.getAcceso() != ACCESO_ELIMINADO)
                .filter(rp -> rp.getPermiso().getAcceso() != ACCESO_ELIMINADO)
                .filter(rp -> rp.getPermiso().getModulo().getAcceso() != ACCESO_ELIMINADO)
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

    public PermisosRolResponse obtenerPermisosDelUsuario(Integer idUsuario) {
        PermisosRolResponse response = new PermisosRolResponse();
        response.setModulos(new ArrayList<>());

        List<RolPermiso> rolPermisos = rolPermisosDelUsuario(idUsuario);
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
                if (rolPermisoAccion.getAcceso() == ACCESO_ACTIVO
                        && rolPermisoAccion.getAccion().getAcceso() != ACCESO_ELIMINADO) {
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

    private List<RolPermiso> rolPermisosDelUsuario(Integer idUsuario) {
        return usuarioRolRepository.findByUsuarioIdUsuario(idUsuario).stream()
                .map(UsuarioRol::getRol)
                .filter(rol -> rol.getAcceso() != ACCESO_ELIMINADO)
                .flatMap(rol -> rolPermisoRepository.findByRolIdRol(rol.getIdRol()).stream())
                .filter(rp -> rp.getAcceso() == ACCESO_ACTIVO)
                .filter(rp -> rp.getPermiso().getAcceso() != ACCESO_ELIMINADO)
                .filter(rp -> rp.getPermiso().getModulo().getAcceso() != ACCESO_ELIMINADO)
                .toList();
    }

    private void asignarAccionesConcedidas(RolPermiso rolPermiso, List<String> codigosAcciones) {
        if (codigosAcciones == null || codigosAcciones.isEmpty()) {
            return;
        }
        validarAccionesSoportadas(rolPermiso, codigosAcciones);
        for (String codigo : codigosAcciones) {
            Accion accion = findAccion(codigo);
            if (rolPermisoAccionRepository.findByRolPermisoIdRolPermisoAndAccionIdAccionAndAccesoNot(rolPermiso.getIdRolPermiso(), accion.getIdAccion(), ACCESO_ELIMINADO).isEmpty()) {
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

    private void revalidarAccionesContraPermiso(RolPermiso rolPermiso) {
        List<String> actuales = accionesConcedidasDeRolPermiso(rolPermiso.getIdRolPermiso());
        Set<String> soportadas = accionesSoportadas(rolPermiso.getPermiso().getIdPermiso());
        List<String> validas = actuales.stream().filter(soportadas::contains).toList();
        if (!validas.equals(actuales)) {
            reemplazarAccionesConcedidas(rolPermiso, validas);
        }
    }

    private Set<String> accionesSoportadas(Integer idPermiso) {
        return permisoAccionRepository.findByPermisoIdPermisoAndAccesoNot(idPermiso, ACCESO_ELIMINADO).stream()
                .filter(pa -> pa.getAccion().getAcceso() != ACCESO_ELIMINADO)
                .map(pa -> pa.getAccion().getCodigo())
                .collect(Collectors.toSet());
    }

    private void crearAccionConcedida(RolPermiso rolPermiso, Accion accion) {
        RolPermisoAccion rolPermisoAccion = new RolPermisoAccion();
        rolPermisoAccion.setRolPermiso(rolPermiso);
        rolPermisoAccion.setAccion(accion);
        rolPermisoAccion.setAcceso(ACCESO_ACTIVO);
        rolPermisoAccionRepository.save(rolPermisoAccion);
    }

    private Accion findAccion(String codigo) {
        return accionRepository.findByCodigoAndAccesoNot(codigo, ACCESO_ELIMINADO)
                .orElseThrow(() -> new IllegalArgumentException("La acción " + codigo + " no existe"));
    }

    private Rol findRol(Integer id) {
        return rolRepository.findByIdRolAndAccesoNot(id, ACCESO_ELIMINADO)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id " + id));
    }

    private Permiso findPermiso(Integer id) {
        return permisoRepository.findByIdPermisoAndAccesoNot(id, ACCESO_ELIMINADO)
                .orElseThrow(() -> new ResourceNotFoundException("Permiso no encontrado con id " + id));
    }

    private RolPermiso findRolPermiso(Integer id) {
        return rolPermisoRepository.findByIdRolPermisoAndAccesoNot(id, ACCESO_ELIMINADO)
                .orElseThrow(() -> new ResourceNotFoundException("RolPermiso no encontrado con id " + id));
    }

    private List<String> accionesDisponiblesDelPermiso(Integer idPermiso) {
        return permisoAccionRepository.findByPermisoIdPermisoAndAccesoNot(idPermiso, ACCESO_ELIMINADO).stream()
                .filter(pa -> pa.getAccion().getAcceso() != ACCESO_ELIMINADO)
                .map(pa -> pa.getAccion().getCodigo())
                .sorted()
                .toList();
    }

    private List<String> accionesConcedidasDeRolPermiso(Integer idRolPermiso) {
        return rolPermisoAccionRepository.findByRolPermisoIdRolPermisoAndAccesoNot(idRolPermiso, ACCESO_ELIMINADO).stream()
                .filter(ra -> ra.getAccion().getAcceso() != ACCESO_ELIMINADO)
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
        response.setAcceso(rolPermiso.getAcceso());
        return response;
    }

    private RolResponse toRolResponse(Rol rol) {
        RolResponse response = new RolResponse();
        response.setIdRol(rol.getIdRol());
        response.setNombre(rol.getNombre());
        response.setAcceso(rol.getAcceso());
        return response;
    }

    private PermisoResponse toPermisoResponse(Permiso permiso) {
        PermisoResponse response = new PermisoResponse();
        response.setIdPermiso(permiso.getIdPermiso());
        response.setCodigo(permiso.getCodigo());
        response.setNombre(permiso.getNombre());
        response.setAcceso(permiso.getAcceso());
        response.setAcciones(accionesDisponiblesDelPermiso(permiso.getIdPermiso()));
        return response;
    }
}
