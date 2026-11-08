package com.example.Escolar.Model;

/** En cuántos periodos se divide el año de un nivel. */
public enum TipoPeriodo {
    BIMESTRE(4, "bimestre"),
    TRIMESTRE(3, "trimestre");

    private static final String[] ORDINALES = {"Primer", "Segundo", "Tercer", "Cuarto"};

    private final int cantidad;
    private final String nombre;

    TipoPeriodo(int cantidad, String nombre) {
        this.cantidad = cantidad;
        this.nombre = nombre;
    }

    public int getCantidad() {
        return cantidad;
    }

    /** "Primer bimestre", "Tercer trimestre". */
    public String nombre(int numero) {
        return ORDINALES[numero - 1] + " " + nombre;
    }
}
