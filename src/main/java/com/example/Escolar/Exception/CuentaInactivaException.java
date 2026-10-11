package com.example.Escolar.Exception;

public class CuentaInactivaException extends RuntimeException {
    public CuentaInactivaException() {
        super("Tu usuario está inactivo. Comunícate con el administrador.");
    }
}
