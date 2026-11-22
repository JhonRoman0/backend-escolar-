package com.example.Escolar.Security;

import com.example.Escolar.Controller.AuthController;
import com.example.Escolar.Exception.VigenciaNoValidaException;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Repository.UsuarioVigenciaSesion;
import com.example.Escolar.Service.AccesoConstants;
import com.example.Escolar.Service.VigenciaService;
import com.example.Escolar.Service.VigenciaService.EstadoVigencia;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String CODIGO_CAMBIO_PENDIENTE = "CAMBIO_CONTRASENA_REQUERIDO";
    private static final Set<String> RUTAS_CON_CAMBIO_PENDIENTE = Set.of(
            "/auth/me", "/auth/change-password", "/auth/permisos");
    private static final Set<String> RUTAS_CON_VIGENCIA_BLOQUEADA = Set.of("/auth/me", "/auth/logout");
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtService jwtService;
    private final ObjectProvider<UsuarioRepository> usuarioRepositoryProvider;
    private final VigenciaService vigenciaService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (rechazarCsrfPorCookie(request)) {
            escribir403(response, null, "Origen no autorizado", null, null, null, false);
            return;
        }

        String token = extraerToken(request);
        Claims claims = token == null ? null : parsearToken(token);
        if (claims != null) {
            UsuarioAutenticado principal = new UsuarioAutenticado(
                    Integer.valueOf(claims.getSubject()),
                    claims.get("codigo", String.class),
                    claims.get("roles", List.class));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + principal.codigo())));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String ruta = request.getServletPath();
            if (!permitidaConVigenciaBloqueada(ruta)) {
                VigenciaNoValidaException bloqueo = validarVigencia(claims);
                if (bloqueo != null) {
                    EstadoVigencia estado = bloqueo.getEstado();
                    escribir403(response, bloqueo.getCodigo(), bloqueo.getMessage(), bloqueo.getFechaInicio(),
                            bloqueo.getFechaFin(), estado == null ? null : estado.name(), true);
                    return;
                }
            }

            if (jwtService.isCambioContrasenaPendiente(claims) && !permitidaConCambioPendiente(ruta)) {
                escribir403(response, CODIGO_CAMBIO_PENDIENTE,
                        "Debe cambiar su contraseña antes de continuar", null, null, null, false);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private Claims parsearToken(String token) {
        try {
            return jwtService.parse(token);
        } catch (Exception e) {
            return null;
        }
    }

    private VigenciaNoValidaException validarVigencia(Claims claims) {
        Integer idUsuario = Integer.valueOf(claims.getSubject());
        Optional<UsuarioVigenciaSesion> sesion = usuarioRepositoryProvider.getObject()
                .findVigenciaSesion(idUsuario, AccesoConstants.ELIMINADO);
        if (sesion.isEmpty()) {
            return new VigenciaNoValidaException(VigenciaNoValidaException.CODIGO_NO_DISPONIBLE,
                    VigenciaNoValidaException.MENSAJE_NO_DISPONIBLE);
        }
        UsuarioVigenciaSesion datos = sesion.get();
        Integer versionToken = jwtService.getVersionToken(claims);
        Integer versionActual = datos.getVersionToken() == null ? 0 : datos.getVersionToken();
        if (!Objects.equals(versionActual, versionToken)) {
            return new VigenciaNoValidaException(VigenciaNoValidaException.CODIGO_TOKEN_OBSOLETO,
                    "Su sesión quedó obsoleta. Vuelva a iniciar sesión.");
        }
        LocalDate fechaInicio = datos.getFechaInicioVigencia();
        LocalDate fechaFin = datos.getFechaFinVigencia();
        if (vigenciaService.datosNoDisponibles(fechaInicio, fechaFin)) {
            return new VigenciaNoValidaException(VigenciaNoValidaException.CODIGO_NO_DISPONIBLE,
                    VigenciaNoValidaException.MENSAJE_NO_DISPONIBLE);
        }
        EstadoVigencia estado = vigenciaService.evaluar(fechaInicio, fechaFin);
        if (!vigenciaService.esValida(estado)) {
            return new VigenciaNoValidaException(estado, fechaInicio, fechaFin);
        }
        return null;
    }

    private void escribir403(HttpServletResponse response, String codigo, String mensaje,
                             LocalDate fechaInicio, LocalDate fechaFin, String estado, boolean incluirVigencia)
            throws IOException {
        response.setStatus(403);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        StringBuilder json = new StringBuilder("{\"status\":403,\"error\":\"Forbidden\"");
        if (codigo != null) {
            json.append(",\"codigo\":\"").append(escapeJson(codigo)).append('"');
        }
        if (mensaje != null) {
            json.append(",\"message\":\"").append(escapeJson(mensaje)).append('"');
        }
        if (incluirVigencia) {
            json.append(",\"fechaInicio\":").append(fechaJson(fechaInicio));
            json.append(",\"fechaFin\":").append(fechaJson(fechaFin));
            json.append(",\"estado\":").append(estado == null ? "null" : "\"" + escapeJson(estado) + "\"");
        }
        json.append('}');
        response.getWriter().write(json.toString());
    }

    private String fechaJson(LocalDate fecha) {
        return fecha == null ? "null" : "\"" + fecha + "\"";
    }

    private String escapeJson(String valor) {
        return valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean permitidaConVigenciaBloqueada(String ruta) {
        return permitida(ruta, RUTAS_CON_VIGENCIA_BLOQUEADA);
    }

    private boolean permitidaConCambioPendiente(String ruta) {
        return permitida(ruta, RUTAS_CON_CAMBIO_PENDIENTE);
    }

    private boolean permitida(String ruta, Set<String> rutasPermitidas) {
        return rutasPermitidas.contains(ruta)
                || SecurityConfig.RUTAS_PUBLICAS.stream().anyMatch(patron -> PATH_MATCHER.match(patron, ruta));
    }

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
