package com.example.Escolar.Util;

/** Convierte el User-Agent en un texto corto y legible: "Chrome en Windows". */
public final class DescripcionNavegador {

    private DescripcionNavegador() {
    }

    public static String de(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Navegador desconocido";
        }
        return navegador(userAgent) + " en " + sistema(userAgent);
    }

    // El orden importa: Edge y Opera también dicen "Chrome", y Chrome dice "Safari".
    private static String navegador(String ua) {
        if (ua.contains("Edg/") || ua.contains("EdgA/") || ua.contains("EdgiOS/")) return "Edge";
        if (ua.contains("OPR/") || ua.contains("Opera")) return "Opera";
        if (ua.contains("SamsungBrowser/")) return "Samsung Internet";
        if (ua.contains("Firefox/") || ua.contains("FxiOS/")) return "Firefox";
        if (ua.contains("Chrome/") || ua.contains("CriOS/")) return "Chrome";
        if (ua.contains("Safari/")) return "Safari";
        return "Navegador";
    }

    private static String sistema(String ua) {
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone")) return "iPhone";
        if (ua.contains("iPad")) return "iPad";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac OS X") || ua.contains("Macintosh")) return "Mac";
        if (ua.contains("CrOS")) return "ChromeOS";
        if (ua.contains("Linux")) return "Linux";
        return "otro sistema";
    }
}
