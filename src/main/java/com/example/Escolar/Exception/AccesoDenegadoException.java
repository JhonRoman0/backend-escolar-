package com.example.Escolar.Exception;

/** La acción existe y el usuario tiene el permiso del módulo, pero esta operación no le corresponde (403). */
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
