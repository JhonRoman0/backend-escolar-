package com.example.Escolar.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerificarCodigoRequest {
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es valido")
    private String gmail;
    @NotBlank(message = "El codigo es obligatorio")
    @Pattern(regexp = "^[0-9]{6}$", message = "El codigo debe ser de 6 digitos")
    private String codigo;
}