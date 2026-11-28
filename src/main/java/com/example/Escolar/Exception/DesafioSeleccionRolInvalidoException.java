package com.example.Escolar.Exception;

public class DesafioSeleccionRolInvalidoException extends RuntimeException {

    public static final String CODIGO = "SELECCION_ROL_DESAFIO_INVALIDO";

    public DesafioSeleccionRolInvalidoException(String mensaje) {
        super(mensaje);
    }
}
