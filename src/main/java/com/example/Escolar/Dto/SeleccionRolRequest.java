package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeleccionRolRequest {
    @NotBlank(message = "La verificación no es válida. Vuelve a iniciar sesión.")
    private String desafio;
    @NotNull(message = "Selecciona un rol")
    private Integer idRol;
}
