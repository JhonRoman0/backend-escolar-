package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CursoCopiaRequest {
    @NotNull(message = "Selecciona el año de origen")
    private Integer idAnioOrigen;
    @NotNull(message = "Selecciona el año de destino")
    private Integer idAnioDestino;
    @NotNull(message = "Selecciona el nivel")
    private Integer idNivel;
}
