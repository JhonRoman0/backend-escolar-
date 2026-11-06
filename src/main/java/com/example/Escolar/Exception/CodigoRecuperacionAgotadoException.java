package com.example.Escolar.Exception;

public class CodigoRecuperacionAgotadoException extends RuntimeException {
    public static final String CODIGO = "CODIGO_RECUPERACION_AGOTADO";

    public CodigoRecuperacionAgotadoException() {
        super("Superaste el número de intentos. Solicita un nuevo código.");
    }
}
