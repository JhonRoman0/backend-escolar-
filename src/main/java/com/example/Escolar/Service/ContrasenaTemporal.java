package com.example.Escolar.Service;

import java.security.SecureRandom;

/** Contraseña que genera el sistema: se envía por correo y se exige cambiarla al ingresar. */
public final class ContrasenaTemporal {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String MAYUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijkmnpqrstuvwxyz";
    private static final String NUMEROS = "23456789";
    private static final String ESPECIALES = "#$%&*@";
    private static final String TODOS = MAYUSCULAS + MINUSCULAS + NUMEROS + ESPECIALES;
    private static final int LARGO = 12;

    private ContrasenaTemporal() {
    }

    // Cumple la política: mayúscula, número y carácter especial, en posiciones al azar.
    public static String generar() {
        char[] clave = new char[LARGO];
        clave[0] = alAzar(MAYUSCULAS);
        clave[1] = alAzar(MINUSCULAS);
        clave[2] = alAzar(NUMEROS);
        clave[3] = alAzar(ESPECIALES);
        for (int i = 4; i < LARGO; i++) {
            clave[i] = alAzar(TODOS);
        }
        for (int i = LARGO - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temporal = clave[i];
            clave[i] = clave[j];
            clave[j] = temporal;
        }
        return new String(clave);
    }

    private static char alAzar(String caracteres) {
        return caracteres.charAt(RANDOM.nextInt(caracteres.length()));
    }
}
