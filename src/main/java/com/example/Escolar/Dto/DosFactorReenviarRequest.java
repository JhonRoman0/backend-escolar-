package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DosFactorReenviarRequest {
    @NotBlank(message = "La verificación no es válida. Vuelve a iniciar sesión.")
    private String desafio;
}
