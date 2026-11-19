package com.example.Escolar.Security;

import com.example.Escolar.Service.EventosService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Cada vez que algo se guarda bien (POST, PUT, PATCH o DELETE con respuesta 2xx)
 * avisa a los navegadores conectados qué módulo cambió, tomado de la ruta
 * (/asistencias/... → "asistencias").
 */
@Component
@RequiredArgsConstructor
public class AvisoCambiosFilter extends OncePerRequestFilter {

    private static final Set<String> MUTANTES = Set.of("POST", "PUT", "PATCH", "DELETE");
    /** Rutas que no cambian datos aunque usen POST. */
    private static final List<String> SIN_AVISO = List.of("/auth/", "/eventos", "/asistencias/previsualizar");

    private final EventosService eventosService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        chain.doFilter(request, response);
        String modulo = moduloQueCambio(request.getMethod(), ruta(request), response.getStatus());
        if (modulo != null) {
            eventosService.avisar(modulo);
        }
    }

    /** El módulo a avisar, o null si la petición no cambió nada. */
    static String moduloQueCambio(String metodo, String ruta, int estado) {
        if (!MUTANTES.contains(metodo) || estado < 200 || estado >= 300 || ruta == null) {
            return null;
        }
        if (SIN_AVISO.stream().anyMatch(ruta::startsWith)) {
            return null;
        }
        String[] partes = ruta.split("/");
        return partes.length > 1 && !partes[1].isBlank() ? partes[1] : null;
    }

    private static String ruta(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contexto = request.getContextPath();
        return contexto != null && !contexto.isEmpty() && uri.startsWith(contexto) ? uri.substring(contexto.length()) : uri;
    }
}
