package com.example.Escolar.Util;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class TextoBusqueda {

    public static final String ESCAPE_LIKE = "!";
    public static final String ESCAPE_CLAUSE = " ESCAPE '" + ESCAPE_LIKE + "'";

    private TextoBusqueda() {
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    public static List<String> tokens(String texto) {
        String normalizado = normalizar(texto);
        return normalizado.isEmpty() ? List.of() : Arrays.asList(normalizado.split("\\s+"));
    }

    public static String patronLike(String token) {
        String escapado = token
                .replace(ESCAPE_LIKE, ESCAPE_LIKE + ESCAPE_LIKE)
                .replace("%", ESCAPE_LIKE + "%")
                .replace("_", ESCAPE_LIKE + "_");
        return "%" + escapado + "%";
    }
}
