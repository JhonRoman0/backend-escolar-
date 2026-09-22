package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlantillaSiagieResponse {
    private Integer idPlantilla;
    private String anio;
    private boolean vigente;
    private String nombreArchivo;
}
