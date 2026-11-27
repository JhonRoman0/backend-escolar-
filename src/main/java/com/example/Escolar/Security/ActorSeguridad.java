package com.example.Escolar.Security;

import com.example.Escolar.Model.UsuarioRol;
import com.example.Escolar.Repository.UsuarioRolRepository;
import com.example.Escolar.Service.AccesoConstants;
import com.example.Escolar.Service.RolConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Quién hace la petición en Seguridad. Los roles se leen de la base (no de la
 * sesión) para que un ADMIN quitado no conserve su poder.
 */
@Component
@RequiredArgsConstructor
public class ActorSeguridad {

    private final UsuarioRolRepository usuarioRolRepository;

    /** Usuario de la sesión; vacío en el arranque o en procesos automáticos. */
    public Optional<Integer> idActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado principal) {
            return Optional.ofNullable(principal.idUsuario());
        }
        return Optional.empty();
    }

    /** ADMIN, o el propio sistema (sin sesión): no tiene las restricciones de Seguridad. */
    public boolean esAdminOSistema() {
        return idActual().map(this::esAdmin).orElse(true);
    }

    public boolean esAdmin(Integer idUsuario) {
        return rolesActivos(idUsuario).stream().anyMatch(ur -> RolConstants.esAdmin(ur.getRol().getNombre()));
    }

    /** Roles activos del usuario de la sesión (vacío si no hay sesión). */
    public Set<Integer> idsRolesActuales() {
        return idActual().map(id -> rolesActivos(id).stream()
                        .map(ur -> ur.getRol().getIdRol()).collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    private List<UsuarioRol> rolesActivos(Integer idUsuario) {
        return usuarioRolRepository.findByUsuarioIdUsuario(idUsuario).stream()
                .filter(ur -> ur.getRol() != null
                        && AccesoConstants.ACTIVO.equals(ur.getRol().getAcceso().getIdAcceso()))
                .toList();
    }
}
