package com.example.Escolar.Security;

import com.example.Escolar.Util.FechaColegio;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceVigenciaTest {

    private static final String SECRETO = "clave-secreta-de-pruebas-con-32-bytes-o-mas!!";

    private final JwtService jwtService = new JwtService(SECRETO, 3_600_000);

    @Test
    void tokenIncluyeVersionYExpiraComoMaximoAlDiaSiguienteDelFin() {
        var fin = FechaColegio.hoy().plusDays(30);
        JwtService service = new JwtService(SECRETO, 60L * 24 * 60 * 60 * 1000);

        String token = service.generarToken(1, "ADM20240001", List.of("ADMIN"), false, fin, 4);
        Claims claims = service.parse(token);

        assertEquals(4, service.getVersionToken(token));
        Instant limite = fin.plusDays(1).atStartOfDay(FechaColegio.ZONA).toInstant();
        assertEquals(limite, claims.getExpiration().toInstant());
        assertTrue(claims.getExpiration().after(new Date()));
    }

    @Test
    void overloadAntiginalGuardaVersionCero() {
        String token = jwtService.generarToken(1, "ADM20240001", List.of("ADMIN"));
        assertEquals(0, jwtService.getVersionToken(token));
    }

    @Test
    void tokenIncluyeIdRolActivoYRolesConUnSoloElemento() {
        String token = jwtService.generarToken(1, "ADM20240001", 7, List.of("Docente"), false, null, 0, 40);

        Claims claims = jwtService.parse(token);

        assertEquals(7, jwtService.getIdRolActivo(claims));
        assertEquals(List.of("Docente"), claims.get("roles", List.class));
        assertEquals(40, jwtService.getDispositivo(claims));
    }

    @Test
    void tokenSinClaimDeVersionDevuelveCero() {
        Instant ahora = Instant.now();
        String token = Jwts.builder()
                .subject("1")
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertEquals(0, jwtService.getVersionToken(token));
    }
}
