package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambiarRolRequest {
    @NotNull(message = "Selecciona un rol")
    private Integer idRol;
}
