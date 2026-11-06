package com.example.Escolar.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es valido")
    private String gmail;
    @NotBlank(message = "El codigo es obligatorio")
    @Pattern(regexp = "^[0-9]{6}$", message = "El codigo debe ser de 6 digitos")
    private String codigo;
    @NotBlank(message = "La nueva contrasena es obligatoria")
    @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
            message = "La contrasena debe tener al menos 1 mayuscula, 1 numero y 1 caracter especial")
    private String nuevaContrasena;
}
