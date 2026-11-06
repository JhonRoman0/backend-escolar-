package com.example.Escolar.Dto;

public class ItemDeleteError {
    private Integer idGradoSeccion;
    private String nombre;
    private String motivo;

    public ItemDeleteError(Integer idGradoSeccion, String nombre, String motivo) {
        this.idGradoSeccion = idGradoSeccion;
        this.nombre = nombre;
        this.motivo = motivo;
    }

    public Integer getIdGradoSeccion() {
        return idGradoSeccion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getMotivo() {
        return motivo;
    }
}
