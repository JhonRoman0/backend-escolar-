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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class RbacAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    public static final byte ACCESO_ACTIVO = 1;
    public static final byte ACCESO_ELIMINADO = 2;

    private final ApiPermisoRegistro apiPermisoRegistro;
    private final UsuarioRolRepository usuarioRolRepository;
    private final RolPermisoRepository rolPermisoRepository;
    private final RolPermisoAccionRepository rolPermisoAccionRepository;

    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication, RequestAuthorizationContext object) {
        ApiPermisoRegistro.Requisito requisito = apiPermisoRegistro.buscar(
                object.getRequest().getMethod(), object.getRequest().getRequestURI()).orElse(null);

        // Endpoint no registrado: negar (fail closed) -> 401/403
        if (requisito == null) {
            return new AuthorizationDecision(false);
        }

        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UsuarioAutenticado principal)) {
            return new AuthorizationDecision(false);
        }

        // Super-rol ADMIN: acceso total a todas las pantallas y acciones
        if (esAdmin(principal)) {
            return new AuthorizationDecision(true);
        }

        Set<String> permisos = new HashSet<>();
        Map<String, Set<String>> accionesPorPermiso = new HashMap<>();
        cargarConcesiones(principal.idUsuario(), permisos, accionesPorPermiso);

        if (!permisos.contains(requisito.permisoCodigo())) {
            return new AuthorizationDecision(false);
        }
        if (requisito.accionCodigo() != null) {
            Set<String> acciones = accionesPorPermiso.get(requisito.permisoCodigo());
            if (acciones == null || !acciones.contains(requisito.accionCodigo())) {
                return new AuthorizationDecision(false);
            }
        }
        return new AuthorizationDecision(true);
    }

    private boolean esAdmin(UsuarioAutenticado principal) {
        return principal.roles().stream().anyMatch(nombre -> "ADMIN".equalsIgnoreCase(nombre));
    }

    private void cargarConcesiones(Integer idUsuario, Set<String> permisos, Map<String, Set<String>> accionesPorPermiso) {
        for (UsuarioRol usuarioRol : usuarioRolRepository.findByUsuarioIdUsuario(idUsuario)) {
            Rol rol = usuarioRol.getRol();
            if (rol.getAcceso() == ACCESO_ELIMINADO) {
                continue;
            }
            for (RolPermiso rolPermiso : rolPermisoRepository.findByRolIdRol(rol.getIdRol())) {
                if (rolPermiso.getAcceso() != ACCESO_ACTIVO) {
                    continue;
                }
                Permiso permiso = rolPermiso.getPermiso();
                if (permiso.getAcceso() == ACCESO_ELIMINADO) {
                    continue;
                }
                permisos.add(permiso.getCodigo());
                Set<String> acciones = accionesPorPermiso.computeIfAbsent(permiso.getCodigo(), k -> new HashSet<>());
                for (RolPermisoAccion rolPermisoAccion : rolPermisoAccionRepository
                        .findByRolPermisoIdRolPermiso(rolPermiso.getIdRolPermiso())) {
                    if (rolPermisoAccion.getAcceso() != ACCESO_ACTIVO) {
                        continue;
                    }
                    Accion accion = rolPermisoAccion.getAccion();
                    if (accion.getAcceso() == ACCESO_ELIMINADO) {
                        continue;
                    }
                    acciones.add(accion.getCodigo());
                }
            }
        }
    }
}
