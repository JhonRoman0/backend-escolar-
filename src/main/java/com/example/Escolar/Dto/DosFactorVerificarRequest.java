package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DosFactorVerificarRequest {
    @NotBlank(message = "La verificación no es válida. Vuelve a iniciar sesión.")
    private String desafio;
    @NotBlank(message = "El codigo es obligatorio")
    @Pattern(regexp = "^[0-9]{6}$", message = "El codigo debe ser de 6 digitos")
    private String codigo;
    /** Recordar este navegador unos días para no pedir el código. */
    private Boolean recordar;
}
