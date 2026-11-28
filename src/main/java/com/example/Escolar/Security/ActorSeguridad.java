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
 * Quién hace la petición en Seguridad. El ADMIN de la sesión se decide por su rol
 * activo vigente; las consultas sobre otro usuario leen la base.
 */
@Component
@RequiredArgsConstructor
public class ActorSeguridad {

    private final UsuarioRolRepository usuarioRolRepository;
    private final ConcesionesService concesionesService;

    /** Usuario de la sesión; vacío en el arranque o en procesos automáticos. */
    public Optional<Integer> idActual() {
        return principalActual().map(UsuarioAutenticado::idUsuario);
    }

    /** Rol activo de la sesión; vacío si no hay sesión. */
    public Optional<Integer> idRolActual() {
        return principalActual().map(UsuarioAutenticado::idRolActivo);
    }

    public Set<Integer> idsRolesActuales() {
        return idActual().map(id -> rolesActivos(id).stream()
                .map(ur -> ur.getRol().getIdRol()).collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    private Optional<UsuarioAutenticado> principalActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    /** ADMIN por su rol activo, o el propio sistema (sin sesión): no tiene las restricciones de Seguridad. */
    public boolean esAdminOSistema() {
        Optional<Integer> idUsuario = idActual();
        if (idUsuario.isEmpty()) {
            return true;
        }
        return idRolActual()
                .map(idRol -> concesionesService.esAdminActivo(idUsuario.get(), idRol))
                .orElse(false);
    }

    /** Consulta administrativa sobre el usuario indicado: cualquier rol ADMIN vigente lo protege. */
    public boolean esAdmin(Integer idUsuario) {
        return rolesActivos(idUsuario).stream().anyMatch(ur -> RolConstants.esAdmin(ur.getRol().getNombre()));
    }

    private List<UsuarioRol> rolesActivos(Integer idUsuario) {
        return usuarioRolRepository.findByUsuarioIdUsuario(idUsuario).stream()
                .filter(ur -> ur.getRol() != null
                        && AccesoConstants.ACTIVO.equals(ur.getRol().getAcceso().getIdAcceso()))
                .toList();
    }
}
