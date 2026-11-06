package com.example.Escolar.Service;

public final class RolConstants {
    public static final String ADMIN = "ADMIN";

    public static boolean esAdmin(String nombre) {
        return nombre != null && ADMIN.equalsIgnoreCase(nombre.trim());
    }

    private RolConstants() {}
}
