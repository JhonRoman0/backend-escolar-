package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class DeleteSeccionesLoteRequest {
    @NotEmpty(message = "Debes seleccionar al menos una sección para eliminar")
    private List<Integer> ids;

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(List<Integer> ids) {
        this.ids = ids;
    }
}
