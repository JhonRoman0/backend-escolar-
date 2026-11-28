package com.example.Escolar.Security;

import com.example.Escolar.Dto.RolAsignadoSesionResponse;
import com.example.Escolar.Dto.RolAsignadoSesionResponse.EstadoRol;
import com.example.Escolar.Model.Accion;
import com.example.Escolar.Model.Permiso;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Model.RolPermiso;
import com.example.Escolar.Model.RolPermisoAccion;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Model.UsuarioRol;
import com.example.Escolar.Repository.RolPermisoAccionRepository;
import com.example.Escolar.Repository.RolPermisoRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import com.example.Escolar.Service.AccesoConstants;
import com.example.Escolar.Service.RolAsignadoResolver;
import com.example.Escolar.Service.RolConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Permisos del rol activo del usuario con las acciones concedidas en él.
 * Una misma petición los consulta varias veces (autorización y alcance), así que se recuerdan
 * unos segundos: un cambio en Accesos se aplica, como mucho, VIGENCIA_MS después.
 */
@Service
@RequiredArgsConstructor
public class ConcesionesService {

    private final UsuarioRolRepository usuarioRolRepository;
    private final RolPermisoRepository rolPermisoRepository;
    private final RolPermisoAccionRepository rolPermisoAccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolAsignadoResolver rolAsignadoResolver;

    static final long VIGENCIA_MS = 5_000;

    private record RolActivo(Map<String, Set<String>> acciones, String nombre, boolean admin) {
    }

    private record Guardado(long hasta, Map<Integer, RolActivo> rolesPorId) {
    }

    private static final class ConstruccionRol {
        private final Map<String, Set<String>> acciones = new HashMap<>();
        private String nombre;
        private boolean admin;
    }

    private final Map<Integer, Guardado> recientes = new ConcurrentHashMap<>();

    /** Acciones del rol activo, sin mezclar permisos de otros roles del usuario. */
    public Map<String, Set<String>> accionesPorPermiso(Integer idUsuario, Integer idRolActivo) {
        RolActivo rol = rolActivo(idUsuario, idRolActivo);
        return rol == null ? Map.of() : rol.acciones();
    }

    public boolean rolActivoValido(Integer idUsuario, Integer idRolActivo) {
        return rolActivo(idUsuario, idRolActivo) != null;
    }

    public boolean esAdminActivo(Integer idUsuario, Integer idRolActivo) {
        RolActivo rol = rolActivo(idUsuario, idRolActivo);
        return rol != null && rol.admin();
    }

    public String nombreRolActivo(Integer idUsuario, Integer idRolActivo) {
        RolActivo rol = rolActivo(idUsuario, idRolActivo);
        return rol == null ? null : rol.nombre();
    }

    private RolActivo rolActivo(Integer idUsuario, Integer idRolActivo) {
        if (idUsuario == null || idRolActivo == null) {
            return null;
        }
        return calculoVigente(idUsuario).get(idRolActivo);
    }

    private Map<Integer, RolActivo> calculoVigente(Integer idUsuario) {
        if (idUsuario == null) {
            return Map.of();
        }
        long ahora = System.currentTimeMillis();
        Guardado guardado = recientes.compute(idUsuario, (id, anterior) ->
                anterior != null && anterior.hasta() > ahora
                        ? anterior : new Guardado(ahora + VIGENCIA_MS, calcular(id)));
        return guardado.rolesPorId();
    }

    public void invalidarCambioRoles(Integer idUsuario) {
        invalidarVinculoDespuesDeCommit(Set.of(idUsuario));
    }

    /** Acumula los usuarios afectados por la misma transaccion. */
    private final class InvalidacionPendiente implements TransactionSynchronization {
        private final Set<Integer> ids = new LinkedHashSet<>();

        @Override
        public void afterCommit() {
            invalidarVinculo(ids);
        }
    }

    public void invalidarVinculoDespuesDeCommit(Collection<Integer> idsUsuarios) {
        if (idsUsuarios == null || idsUsuarios.isEmpty()) {
            return;
        }
        Set<Integer> ids = idsUsuarios.stream().filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            invalidarVinculo(ids);
            return;
        }
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            if (sync instanceof InvalidacionPendiente pendiente) {
                pendiente.ids.addAll(ids);
                return;
            }
        }
        InvalidacionPendiente pendiente = new InvalidacionPendiente();
        pendiente.ids.addAll(ids);
        TransactionSynchronizationManager.registerSynchronization(pendiente);
    }

    private void invalidarVinculo(Set<Integer> ids) {
        subirVersionEnLote(ids);
        ids.forEach(recientes::remove);
    }

    public void invalidarPermisosDelRol(Integer idRol) {
        invalidarCambioPermisos(idRol == null ? Set.of() : Set.of(idRol), Set.of());
    }

    public void invalidarPermisosDeAccion(Integer idAccion) {
        invalidarVinculoDespuesDeCommit(usuarioRolRepository.idsUsuariosConAccion(
                idAccion, AccesoConstants.ACTIVO, AccesoConstants.ELIMINADO));
    }

    public void invalidarPermisosDelPermiso(Integer idPermiso) {
        invalidarCambioPermisos(Set.of(), idPermiso == null ? Set.of() : Set.of(idPermiso));
    }

    public void invalidarCambioPermisos(Collection<Integer> idsRol, Collection<Integer> idsPermiso) {
        Set<Integer> ids = new LinkedHashSet<>();
        if (idsRol != null) {
            for (Integer idRol : idsRol) {
                if (idRol != null) {
                    ids.addAll(idsUsuariosConRol(idRol));
                }
            }
        }
        if (idsPermiso != null) {
            for (Integer idPermiso : idsPermiso) {
                if (idPermiso != null) {
                    ids.addAll(idsUsuariosConPermiso(idPermiso));
                }
            }
        }
        invalidarVinculoDespuesDeCommit(ids);
    }

    private Set<Integer> idsUsuariosConRol(Integer idRol) {
        return usuarioRolRepository.findByRolIdRol(idRol).stream()
                .map(UsuarioRol::getUsuario)
                .filter(Objects::nonNull)
                .filter(this::esVigente)
                .map(Usuario::getIdUsuario)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Integer> idsUsuariosConPermiso(Integer idPermiso) {
        Set<Integer> ids = new LinkedHashSet<>();
        for (RolPermiso rolPermiso : rolPermisoRepository.findByPermisoIdPermiso(idPermiso)) {
            Rol rol = rolPermiso.getRol();
            if (rol != null && rol.getIdRol() != null) {
                ids.addAll(idsUsuariosConRol(rol.getIdRol()));
            }
        }
        return ids;
    }

    private boolean esVigente(Usuario usuario) {
        return usuario.getAcceso() != null
                && !AccesoConstants.ELIMINADO.equals(usuario.getAcceso().getIdAcceso());
    }

    private void subirVersionEnLote(Set<Integer> ids) {
        if (!ids.isEmpty()) {
            usuarioRepository.incrementarVersionToken(ids, AccesoConstants.ELIMINADO);
        }
    }

    private Map<Integer, RolActivo> calcular(Integer idUsuario) {
        Map<Integer, ConstruccionRol> porId = new HashMap<>();
        List<UsuarioRol> vinculos = usuarioRolRepository.findByUsuarioIdUsuario(idUsuario);
        List<Rol> roles = vinculos.stream().map(UsuarioRol::getRol).filter(Objects::nonNull).toList();
        Set<Integer> idsRolesActivos = rolAsignadoResolver.resolver(idUsuario, roles).stream()
                .filter(estado -> estado.estado() == EstadoRol.ACTIVO)
                .map(RolAsignadoSesionResponse::idRol)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (UsuarioRol usuarioRol : vinculos) {
            Rol rol = usuarioRol.getRol();
            if (rol == null || rol.getIdRol() == null || rol.getAcceso() == null
                    || !AccesoConstants.ACTIVO.equals(rol.getAcceso().getIdAcceso()) || rol.getNombre() == null
                    || !idsRolesActivos.contains(rol.getIdRol())) {
                continue;
            }
            String nombreRol = rol.getNombre().toUpperCase(Locale.ROOT);
            ConstruccionRol construccion = porId.computeIfAbsent(rol.getIdRol(), k -> new ConstruccionRol());
            construccion.nombre = rol.getNombre();
            construccion.admin = RolConstants.esAdmin(nombreRol);
            for (RolPermiso rolPermiso : rolPermisoRepository.findByRolIdRol(rol.getIdRol())) {
                if (!AccesoConstants.ACTIVO.equals(rolPermiso.getAcceso().getIdAcceso())) {
                    continue;
                }
                Permiso permiso = rolPermiso.getPermiso();
                if (!AccesoConstants.ACTIVO.equals(permiso.getAcceso().getIdAcceso())
                        || !AccesoConstants.ACTIVO.equals(permiso.getModulo().getAcceso().getIdAcceso())) {
                    continue;
                }
                Set<String> acciones = construccion.acciones
                        .computeIfAbsent(permiso.getCodigo(), k -> new HashSet<>());
                for (RolPermisoAccion rolPermisoAccion : rolPermisoAccionRepository
                        .findByRolPermisoIdRolPermiso(rolPermiso.getIdRolPermiso())) {
                    if (!AccesoConstants.ACTIVO.equals(rolPermisoAccion.getAcceso().getIdAcceso())) {
                        continue;
                    }
                    Accion accion = rolPermisoAccion.getAccion();
                    if (AccesoConstants.ACTIVO.equals(accion.getAcceso().getIdAcceso())) {
                        acciones.add(accion.getCodigo());
                    }
                }
            }
        }
        Map<Integer, RolActivo> resultado = new HashMap<>();
        porId.forEach((idRol, construccion) -> {
            Map<String, Set<String>> acciones = new HashMap<>();
            construccion.acciones.forEach((codigo, permitidas) -> acciones.put(codigo, Set.copyOf(permitidas)));
            resultado.put(idRol, new RolActivo(Map.copyOf(acciones), construccion.nombre, construccion.admin));
        });
        return Map.copyOf(resultado);
    }
}
