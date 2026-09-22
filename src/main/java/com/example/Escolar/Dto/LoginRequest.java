package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "El codigo es obligatorio")
    private String codigo;
    @NotBlank(message = "La contraseña es obligatoria")
    private String contraseña;
}
