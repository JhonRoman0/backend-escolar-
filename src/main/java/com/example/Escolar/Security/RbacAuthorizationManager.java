package com.example.Escolar.Security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class RbacAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final ApiPermisoRegistro apiPermisoRegistro;
    private final ConcesionesService concesionesService;

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

        Map<String, Set<String>> accionesPorPermiso = concesionesService.accionesPorPermiso(principal.idUsuario());

        // Basta con uno de los permisos de la ruta (los catálogos se leen desde varios módulos).
        for (String permisoCodigo : requisito.permisosCodigo()) {
            Set<String> acciones = accionesPorPermiso.get(permisoCodigo);
            if (acciones != null && (requisito.accionCodigo() == null || acciones.contains(requisito.accionCodigo()))) {
                return new AuthorizationDecision(true);
            }
        }
        return new AuthorizationDecision(false);
    }

    private boolean esAdmin(UsuarioAutenticado principal) {
        return principal.roles().stream().anyMatch(nombre -> "ADMIN".equalsIgnoreCase(nombre));
    }
}
