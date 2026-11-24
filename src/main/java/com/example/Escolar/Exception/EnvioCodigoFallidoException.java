package com.example.Escolar.Exception;

/** El correo con el código no se pudo enviar: el usuario no podría continuar. */
public class EnvioCodigoFallidoException extends RuntimeException {
    public EnvioCodigoFallidoException() {
        super("No pudimos enviar el código a tu correo. Intenta en unos minutos o comunícate con el administrador.");
    }
}
