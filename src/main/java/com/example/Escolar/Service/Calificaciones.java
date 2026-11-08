package com.example.Escolar.Service;

import com.example.Escolar.Model.EscalaCalificacion;

import java.util.List;

/** Reglas de una nota según la escala; las usan evaluaciones y notas finales. */
public final class Calificaciones {

    public static final List<String> LITERALES = List.of("AD", "A", "B", "C");
    public static final int MINIMA = 0;
    public static final int MAXIMA = 20;
    /** En vigesimal, desde esta nota hacia abajo se exige conclusión. */
    public static final int MAXIMA_CON_CONCLUSION = 12;
    public static final int CONCLUSION_MINIMA = 10;
    public static final int CONCLUSION_MAXIMA = 500;

    private Calificaciones() {
    }

    /** La nota limpia ("ad" → "AD", "07" → "7") o un error si no vale en la escala. */
    public static String normalizar(EscalaCalificacion escala, String valor) {
        String texto = valor == null ? "" : valor.trim().toUpperCase();
        if (escala == EscalaCalificacion.LITERAL) {
            if (!LITERALES.contains(texto)) {
                throw new IllegalArgumentException("La nota debe ser AD, A, B o C");
            }
            return texto;
        }
        if (!texto.matches("\\d{1,2}") || Integer.parseInt(texto) > MAXIMA) {
            throw new IllegalArgumentException("La nota debe ser un número entero de " + MINIMA + " a " + MAXIMA);
        }
        return String.valueOf(Integer.parseInt(texto));
    }

    /** B o C en literal; 12 o menos en vigesimal. La nota ya viene normalizada. */
    public static boolean requiereConclusion(EscalaCalificacion escala, String calificacion) {
        if (escala == EscalaCalificacion.LITERAL) {
            return "B".equals(calificacion) || "C".equals(calificacion);
        }
        return Integer.parseInt(calificacion) <= MAXIMA_CON_CONCLUSION;
    }

    /** La conclusión limpia (null si va vacía y no se exige) o un error. */
    public static String validarConclusion(EscalaCalificacion escala, String calificacion, String conclusion, String de) {
        String texto = conclusion == null ? "" : conclusion.trim();
        if (texto.isEmpty()) {
            if (requiereConclusion(escala, calificacion)) {
                throw new IllegalArgumentException("Falta la conclusión descriptiva de " + de
                        + ": es obligatoria con " + (escala == EscalaCalificacion.LITERAL ? "B o C" : "12 o menos"));
            }
            return null;
        }
        if (texto.length() < CONCLUSION_MINIMA || texto.length() > CONCLUSION_MAXIMA) {
            throw new IllegalArgumentException("La conclusión descriptiva de " + de + " debe tener entre "
                    + CONCLUSION_MINIMA + " y " + CONCLUSION_MAXIMA + " caracteres");
        }
        return texto;
    }

    /** Promedio de notas vigesimales redondeado desde ,5 (12,5 → 13). Null si no hay notas. */
    public static Integer promedio(List<String> notas) {
        List<Integer> numeros = notas.stream()
                .filter(n -> n != null && n.matches("\\d{1,2}"))
                .map(Integer::parseInt)
                .toList();
        if (numeros.isEmpty()) {
            return null;
        }
        return java.math.BigDecimal.valueOf(numeros.stream().mapToInt(Integer::intValue).sum())
                .divide(java.math.BigDecimal.valueOf(numeros.size()), 0, java.math.RoundingMode.HALF_UP)
                .intValue();
    }
}
