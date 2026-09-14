package com.example.Escolar.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactoMensajeRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombreRemitente;
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El formato del correo no es válido")
    private String correo;
    private String celular;
    private String asunto;
    @NotBlank(message = "El mensaje es obligatorio")
    private String mensaje;
}