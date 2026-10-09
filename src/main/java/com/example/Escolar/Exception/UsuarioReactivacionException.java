package com.example.Escolar.Exception;

import lombok.Getter;

import java.time.LocalDate;

/**
 * Se lanza al intentar crear un usuario cuyo DNI ya pertenece a un usuario
 * eliminado logicamente. Transporta los datos del registro anterior para que
 * el cliente pueda ofrecer reactivarlo en lugar de crear un duplicado.
 */
@Getter
public class UsuarioReactivacionException extends RuntimeException {

    private final Integer idUsuario;
    private final String nombre;
    private final String codigo;
    private final LocalDate fechaCreacion;

    public UsuarioReactivacionException(Integer idUsuario, String nombre, String codigo, LocalDate fechaCreacion) {
        super("Ya existe un usuario eliminado con ese documento de identidad. Se puede reactivar el registro anterior.");
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.codigo = codigo;
        this.fechaCreacion = fechaCreacion;
    }
}