package com.example.Escolar.Security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public static final List<String> RUTAS_PUBLICAS = List.of(
            "/auth/login",
            "/auth/logout",
            "/auth/forgot-password",
            "/auth/verify-reset-code",
            "/auth/reset-password",
            "/portal/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**");

    public static final List<String> RUTAS_SOLO_AUTENTICADAS = List.of(
            "/auth/me",
            "/auth/change-password",
            "/auth/permisos",
            "/consulta/dni/**");

    private final JwtAuthFilter jwtAuthFilter;
    private final RbacAuthorizationManager rbacAuthorizationManager;

    @Value("${app.cors.allowed-origins:http://localhost:*}")
    private List<String> corsOrigenes;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, RbacAuthorizationManager rbacAuthorizationManager) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.rbacAuthorizationManager = rbacAuthorizationManager;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(RUTAS_PUBLICAS.toArray(new String[0])).permitAll()
                        .requestMatchers(RUTAS_SOLO_AUTENTICADAS.toArray(new String[0])).authenticated()
                        .anyRequest().access(rbacAuthorizationManager))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(this::responder401)
                        .accessDeniedHandler(this::responder403))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Orígenes permitidos desde la propiedad app.cors.allowed-origins (separados por coma).
        // En producción el front y la API comparten dominio (/api), así que CORS casi no interviene.
        config.setAllowedOriginPatterns(corsOrigenes);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void responder401(jakarta.servlet.http.HttpServletRequest request,
                              jakarta.servlet.http.HttpServletResponse response,
                              org.springframework.security.core.AuthenticationException ex) throws java.io.IOException {
        response.setStatus(401);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Debe iniciar sesión o el token es inválido o expiró\"}");
    }

    private void responder403(jakarta.servlet.http.HttpServletRequest request,
                              jakarta.servlet.http.HttpServletResponse response,
                              org.springframework.security.access.AccessDeniedException ex) throws java.io.IOException {
        response.setStatus(403);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"No tiene permiso para esta pantalla o acción\"}");
    }
}
