package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AutorizacionRequest {
    @NotNull(message = "Debe indicar el docente al que se asigna el código")
    private Integer idUsuarioDestinatario;
}