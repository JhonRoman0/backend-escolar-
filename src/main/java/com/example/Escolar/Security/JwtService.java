package com.example.Escolar.Security;

import com.example.Escolar.Util.FechaColegio;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtService {

    public static final String CLAIM_CAMBIO_PENDIENTE = "cambioContrasenaPendiente";
    public static final String CLAIM_VERSION_TOKEN = "versionToken";
    public static final String CLAIM_DISPOSITIVO = "dispositivo";
    public static final String CLAIM_ID_ROL_ACTIVO = "idRolActivo";

    private final SecretKey secretKey;
    private final long expiration;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration}") long expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generarToken(Integer idUsuario, String codigo, List<String> roles) {
        return generarToken(idUsuario, codigo, null, roles, false, null, 0, null);
    }

    public String generarToken(Integer idUsuario, String codigo, List<String> roles, boolean cambioContrasenaPendiente) {
        return generarToken(idUsuario, codigo, null, roles, cambioContrasenaPendiente, null, 0, null);
    }

    public String generarToken(Integer idUsuario, String codigo, List<String> roles,
                               boolean cambioContrasenaPendiente, LocalDate fechaFinVigencia, Integer versionToken) {
        return generarToken(idUsuario, codigo, null, roles, cambioContrasenaPendiente, fechaFinVigencia, versionToken, null);
    }

    /** Con idDispositivo, la sesión deja de valer si ese dispositivo se quita de la cuenta. */
    public String generarToken(Integer idUsuario, String codigo, List<String> roles,
                               boolean cambioContrasenaPendiente, LocalDate fechaFinVigencia, Integer versionToken,
                               Integer idDispositivo) {
        return generarToken(idUsuario, codigo, null, roles, cambioContrasenaPendiente, fechaFinVigencia,
                versionToken, idDispositivo);
    }

    public String generarToken(Integer idUsuario, String codigo, Integer idRolActivo, List<String> roles,
                               boolean cambioContrasenaPendiente, LocalDate fechaFinVigencia, Integer versionToken,
                               Integer idDispositivo) {
        Map<String, Object> claims = new HashMap<>();
        if (idRolActivo != null) {
            claims.put(CLAIM_ID_ROL_ACTIVO, idRolActivo);
        }
        if (idDispositivo != null) {
            claims.put(CLAIM_DISPOSITIVO, idDispositivo);
        }
        Date ahora = new Date();
        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(idUsuario))
                .claim("codigo", codigo)
                .claim("roles", roles)
                .claim(CLAIM_CAMBIO_PENDIENTE, cambioContrasenaPendiente)
                .claim(CLAIM_VERSION_TOKEN, versionToken == null ? 0 : versionToken)
                .issuedAt(ahora)
                .expiration(calcularExpiracion(ahora, fechaFinVigencia))
                .signWith(secretKey)
                .compact();
    }

    public boolean isCambioContrasenaPendiente(Claims claims) {
        return Boolean.TRUE.equals(claims.get(CLAIM_CAMBIO_PENDIENTE, Boolean.class));
    }

    /** Dispositivo de la sesión; null en sesiones sin dispositivo (sin doble factor o anteriores). */
    public Integer getDispositivo(Claims claims) {
        Object valor = claims.get(CLAIM_DISPOSITIVO);
        return valor instanceof Number numero ? numero.intValue() : null;
    }

    public Integer getIdRolActivo(Claims claims) {
        Object valor = claims.get(CLAIM_ID_ROL_ACTIVO);
        return valor instanceof Number numero ? numero.intValue() : null;
    }

    public Integer getVersionToken(String token) {
        return getVersionToken(parse(token));
    }

    public Integer getVersionToken(Claims claims) {
        Object valor = claims.get(CLAIM_VERSION_TOKEN);
        return valor instanceof Number numero ? numero.intValue() : 0;
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Date calcularExpiracion(Date ahora, LocalDate fechaFinVigencia) {
        Instant limite = ahora.toInstant().plusMillis(expiration);
        if (fechaFinVigencia != null) {
            Instant fin = fechaFinVigencia.plusDays(1).atStartOfDay(FechaColegio.ZONA).toInstant();
            if (fin.isBefore(limite)) {
                limite = fin;
            }
        }
        return Date.from(limite);
    }
}
