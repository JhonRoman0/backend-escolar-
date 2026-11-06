package com.example.Escolar.Model;

// Lista fija: se guarda el nombre del enum en alumno.sexo y apoderado.sexo.
public enum Sexo {
    MASCULINO("Masculino"),
    FEMENINO("Femenino");

    private final String etiqueta;

    Sexo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
