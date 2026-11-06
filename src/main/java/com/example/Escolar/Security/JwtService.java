package com.example.Escolar.Security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtService {

    private static final String CLAIM_CAMBIO_PENDIENTE = "cambioContrasenaPendiente";

    private final SecretKey secretKey;
    private final long expiration;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration}") long expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generarToken(Integer idUsuario, String codigo, List<String> roles) {
        return generarToken(idUsuario, codigo, roles, false);
    }

    public String generarToken(Integer idUsuario, String codigo, List<String> roles, boolean cambioContrasenaPendiente) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(String.valueOf(idUsuario))
                .claim("codigo", codigo)
                .claim("roles", roles)
                .claim(CLAIM_CAMBIO_PENDIENTE, cambioContrasenaPendiente)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiration))
                .signWith(secretKey)
                .compact();
    }

    public boolean esTokenValido(String token) {
        try {
            extraerClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Integer getIdUsuario(String token) {
        return Integer.valueOf(extraerClaims(token).getSubject());
    }

    public String getCodigo(String token) {
        return extraerClaims(token).get("codigo", String.class);
    }

    public List<String> getRoles(String token) {
        return extraerClaims(token).get("roles", List.class);
    }

    public boolean isCambioContrasenaPendiente(String token) {
        return Boolean.TRUE.equals(extraerClaims(token).get(CLAIM_CAMBIO_PENDIENTE, Boolean.class));
    }

    private io.jsonwebtoken.Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
