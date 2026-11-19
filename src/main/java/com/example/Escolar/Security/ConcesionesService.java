package com.example.Escolar.Security;

import com.example.Escolar.Model.Accion;
import com.example.Escolar.Model.Permiso;
import com.example.Escolar.Model.Rol;
import com.example.Escolar.Model.RolPermiso;
import com.example.Escolar.Model.RolPermisoAccion;
import com.example.Escolar.Model.UsuarioRol;
import com.example.Escolar.Repository.RolPermisoAccionRepository;
import com.example.Escolar.Repository.RolPermisoRepository;
import com.example.Escolar.Repository.UsuarioRolRepository;
import com.example.Escolar.Service.AccesoConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    static final long VIGENCIA_MS = 5_000;

    private record Guardado(long hasta, Map<String, Set<String>> acciones) {
    }

    private final Map<Integer, Guardado> recientes = new ConcurrentHashMap<>();

    public Map<String, Set<String>> accionesPorPermiso(Integer idUsuario) {
        if (idUsuario == null) {
            return new HashMap<>();
        }
        long ahora = System.currentTimeMillis();
        Guardado guardado = recientes.get(idUsuario);
        if (guardado != null && guardado.hasta() > ahora) {
            return copia(guardado.acciones());
        }
        Map<String, Set<String>> acciones = calcular(idUsuario);
        recientes.put(idUsuario, new Guardado(ahora + VIGENCIA_MS, acciones));
        return copia(acciones);
    }

    /** Copia: quien la recibe puede modificarla sin tocar lo guardado. */
    private static Map<String, Set<String>> copia(Map<String, Set<String>> acciones) {
        Map<String, Set<String>> c = new HashMap<>();
        acciones.forEach((k, v) -> c.put(k, new HashSet<>(v)));
        return c;
    }

    private Map<String, Set<String>> calcular(Integer idUsuario) {
        Map<String, Set<String>> accionesPorPermiso = new HashMap<>();
        for (UsuarioRol usuarioRol : usuarioRolRepository.findByUsuarioIdUsuario(idUsuario)) {
            Rol rol = usuarioRol.getRol();
            if (rol.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO)) {
                continue;
            }
            for (RolPermiso rolPermiso : rolPermisoRepository.findByRolIdRol(rol.getIdRol())) {
                if (!rolPermiso.getAcceso().getIdAcceso().equals(AccesoConstants.ACTIVO)) {
                    continue;
                }
                Permiso permiso = rolPermiso.getPermiso();
                if (permiso.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO)) {
                    continue;
                }
                Set<String> acciones = accionesPorPermiso.computeIfAbsent(permiso.getCodigo(), k -> new HashSet<>());
                for (RolPermisoAccion rolPermisoAccion : rolPermisoAccionRepository
                        .findByRolPermisoIdRolPermiso(rolPermiso.getIdRolPermiso())) {
                    if (!rolPermisoAccion.getAcceso().getIdAcceso().equals(AccesoConstants.ACTIVO)) {
                        continue;
                    }
                    Accion accion = rolPermisoAccion.getAccion();
                    if (accion.getAcceso().getIdAcceso().equals(AccesoConstants.ELIMINADO)) {
                        continue;
                    }
                    acciones.add(accion.getCodigo());
                }
            }
        }
        return accionesPorPermiso;
    }
}
