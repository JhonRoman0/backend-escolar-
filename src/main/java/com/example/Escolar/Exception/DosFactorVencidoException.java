package com.example.Escolar.Exception;

/** El inicio de sesión a medias venció o ya no existe: hay que volver a la contraseña. */
public class DosFactorVencidoException extends RuntimeException {
    public static final String CODIGO = "DOS_FACTOR_VENCIDO";

    public DosFactorVencidoException(String mensaje) {
        super(mensaje);
    }
}
