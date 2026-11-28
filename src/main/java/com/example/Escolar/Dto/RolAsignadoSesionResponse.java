package com.example.Escolar.Dto;

public record RolAsignadoSesionResponse(Integer idRol, String nombre, EstadoRol estado) {

    public enum EstadoRol {
        ACTIVO,
        INACTIVO,
        SUSPENDIDO
    }
}
