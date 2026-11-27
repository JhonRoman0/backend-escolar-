package com.example.Escolar.Security;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Permisos activos del usuario (sumando todos sus roles) con las acciones concedidas en cada uno.
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

    static final long VIGENCIA_MS = 5_000;

    private record Calculo(Map<String, Set<String>> acciones,
                           Map<String, Map<String, Set<String>>> accionesPorRol) {
    }

    private record Guardado(long hasta, Calculo calculo) {
    }

    private final Map<Integer, Guardado> recientes = new ConcurrentHashMap<>();

    public Map<String, Set<String>> accionesPorPermiso(Integer idUsuario) {
        return calculoVigente(idUsuario).acciones();
    }

    /** Acciones por rol activo, para no mezclar permisos de un rol con vínculos de otro. */
    public Map<String, Map<String, Set<String>>> accionesPorPermisoYRol(Integer idUsuario) {
        return copiaPorRol(calculoVigente(idUsuario).accionesPorRol());
    }

    private Calculo calculoVigente(Integer idUsuario) {
        if (idUsuario == null) {
            return new Calculo(Map.of(), Map.of());
        }
        long ahora = System.currentTimeMillis();
        Guardado guardado = recientes.compute(idUsuario, (id, anterior) ->
                anterior != null && anterior.hasta() > ahora
                        ? anterior : new Guardado(ahora + VIGENCIA_MS, calcular(id)));
        return new Calculo(copia(guardado.calculo().acciones()), copiaPorRol(guardado.calculo().accionesPorRol()));
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

    /** Copia: quien la recibe puede modificarla sin tocar lo guardado. */
    private static Map<String, Set<String>> copia(Map<String, Set<String>> acciones) {
        Map<String, Set<String>> c = new HashMap<>();
        acciones.forEach((k, v) -> c.put(k, new HashSet<>(v)));
        return c;
    }

    private static Map<String, Map<String, Set<String>>> copiaPorRol(
            Map<String, Map<String, Set<String>>> accionesPorRol) {
        Map<String, Map<String, Set<String>>> copia = new HashMap<>();
        accionesPorRol.forEach((rol, acciones) -> copia.put(rol, copia(acciones)));
        return copia;
    }

    private Calculo calcular(Integer idUsuario) {
        Map<String, Set<String>> accionesPorPermiso = new HashMap<>();
        Map<String, Map<String, Set<String>>> accionesPorRol = new HashMap<>();
        for (UsuarioRol usuarioRol : usuarioRolRepository.findByUsuarioIdUsuario(idUsuario)) {
            Rol rol = usuarioRol.getRol();
            if (!AccesoConstants.ACTIVO.equals(rol.getAcceso().getIdAcceso()) || rol.getNombre() == null) {
                continue;
            }
            String nombreRol = rol.getNombre().toUpperCase(Locale.ROOT);
            Map<String, Set<String>> delRol = accionesPorRol.computeIfAbsent(nombreRol, k -> new HashMap<>());
            for (RolPermiso rolPermiso : rolPermisoRepository.findByRolIdRol(rol.getIdRol())) {
                if (!AccesoConstants.ACTIVO.equals(rolPermiso.getAcceso().getIdAcceso())) {
                    continue;
                }
                Permiso permiso = rolPermiso.getPermiso();
                if (!AccesoConstants.ACTIVO.equals(permiso.getAcceso().getIdAcceso())) {
                    continue;
                }
                if (!AccesoConstants.ACTIVO.equals(permiso.getModulo().getAcceso().getIdAcceso())) {
                    continue;
                }
                Set<String> acciones = delRol.computeIfAbsent(permiso.getCodigo(), k -> new HashSet<>());
                Set<String> todas = accionesPorPermiso.computeIfAbsent(permiso.getCodigo(), k -> new HashSet<>());
                for (RolPermisoAccion rolPermisoAccion : rolPermisoAccionRepository
                        .findByRolPermisoIdRolPermiso(rolPermiso.getIdRolPermiso())) {
                    if (!AccesoConstants.ACTIVO.equals(rolPermisoAccion.getAcceso().getIdAcceso())) {
                        continue;
                    }
                    Accion accion = rolPermisoAccion.getAccion();
                    if (!AccesoConstants.ACTIVO.equals(accion.getAcceso().getIdAcceso())) {
                        continue;
                    }
                    acciones.add(accion.getCodigo());
                    todas.add(accion.getCodigo());
                }
            }
        }
        return new Calculo(accionesPorPermiso, accionesPorRol);
    }
}
