package com.example.Escolar.Dto;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ApoderadoRequest {

    private String nombre;
    private String apellidoPat;
    private String apellidoMat;
    @Email(message = "El formato del email no es válido")
    private String gmail;
    private String contraseña;
    private LocalDate fechaNaci;
    private String documentoIdentidad;
    private String celular;
    private String direccion;
    private String parentesco;
}