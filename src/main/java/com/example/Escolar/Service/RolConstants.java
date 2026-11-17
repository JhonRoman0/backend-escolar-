package com.example.Escolar.Service;

public final class RolConstants {
    public static final String ADMIN = "ADMIN";

    /** Lo asigna la matrícula al crear el usuario del apoderado. */
    public static final String APODERADO = "Apoderado";

    public static boolean esAdmin(String nombre) {
        return nombre != null && ADMIN.equalsIgnoreCase(nombre.trim());
    }

    /** Roles del sistema: no se borran, renombran, desactivan ni duplican. */
    public static boolean esProtegido(String nombre) {
        return esAdmin(nombre) || (nombre != null && APODERADO.equalsIgnoreCase(nombre.trim()));
    }

    private RolConstants() {}
}
