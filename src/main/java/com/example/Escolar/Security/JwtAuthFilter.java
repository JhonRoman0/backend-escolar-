package com.example.Escolar.Security;

import com.example.Escolar.Controller.AuthController;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String CODIGO_CAMBIO_PENDIENTE = "CAMBIO_CONTRASENA_REQUERIDO";
    private static final Set<String> RUTAS_CON_CAMBIO_PENDIENTE = Set.of(
            "/auth/me", "/auth/change-password", "/auth/permisos");
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (rechazarCsrfPorCookie(request)) {
            response.setStatus(403);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Origen no autorizado\"}");
            return;
        }

        String token = extraerToken(request);
        if (token != null && jwtService.esTokenValido(token)) {
            UsuarioAutenticado principal = new UsuarioAutenticado(
                    jwtService.getIdUsuario(token),
                    jwtService.getCodigo(token),
                    jwtService.getRoles(token));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + principal.codigo())));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            if (jwtService.isCambioContrasenaPendiente(token) && !permitidaConCambioPendiente(request.getServletPath())) {
                response.setStatus(403);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"codigo\":\"" + CODIGO_CAMBIO_PENDIENTE
                        + "\",\"message\":\"Debe cambiar su contraseña antes de continuar\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    // Con la contraseña temporal solo se permite lo necesario para cambiarla o salir.
    private boolean permitidaConCambioPendiente(String ruta) {
        return RUTAS_CON_CAMBIO_PENDIENTE.contains(ruta)
                || SecurityConfig.RUTAS_PUBLICAS.stream().anyMatch(patron -> PATH_MATCHER.match(patron, ruta));
    }

    // Con SameSite=None la cookie viaja en requests cross-site: se exige que las
    // peticiones de escritura con sesión por cookie traigan el header custom que el
    // front incluye siempre (el navegador fuerza preflight y un tercero no puede).
    private boolean rechazarCsrfPorCookie(HttpServletRequest request) {
        if ("/auth/logout".equals(request.getServletPath())) {
            return false;
        }
        String authorization = request.getHeader("Authorization");
        boolean traeBearer = authorization != null && authorization.startsWith("Bearer ");
        if (traeBearer) {
            return false;
        }
        String metodo = request.getMethod();
        boolean mutante = "POST".equalsIgnoreCase(metodo) || "PUT".equalsIgnoreCase(metodo)
                || "DELETE".equalsIgnoreCase(metodo) || "PATCH".equalsIgnoreCase(metodo);
        if (!mutante || !tieneCookieAuth(request)) {
            return false;
        }
        return request.getHeader("X-Requested-With") == null;
    }

    private boolean tieneCookieAuth(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }
        return Arrays.stream(cookies).anyMatch(c -> AuthController.COOKIE_NOMBRE.equals(c.getName()));
    }

    private String extraerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(c -> AuthController.COOKIE_NOMBRE.equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}
