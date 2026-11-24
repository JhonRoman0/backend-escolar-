package com.example.Escolar.Util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Hash SHA-256 y valores aleatorios para códigos y tokens que se guardan sin el valor real. */
public final class HashSeguro {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private HashSeguro() {
    }

    public static String sha256(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo generar el hash", e);
        }
    }

    /** Código numérico de 6 dígitos. */
    public static String codigo6Digitos() {
        return String.valueOf(100000 + ALEATORIO.nextInt(900000));
    }

    /** Token aleatorio de 32 bytes, apto para cookies y URLs. */
    public static String tokenAleatorio() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
