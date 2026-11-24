package com.example.Escolar.Exception;

/** Se alcanzó el tope de códigos por hora de un usuario. */
public class LimiteEnviosCodigoException extends RuntimeException {
    public LimiteEnviosCodigoException(long minutosRestantes) {
        super("Ya se enviaron varios códigos a tu correo. Intenta de nuevo en " + minutosRestantes
                + " minuto" + (minutosRestantes == 1 ? "" : "s") + ".");
    }
}
