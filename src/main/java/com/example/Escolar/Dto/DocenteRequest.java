package com.example.Escolar.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DocenteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    @NotBlank(message = "El apellido paterno es obligatorio")
    private String apellidoPat;
    @NotBlank(message = "El apellido materno es obligatorio")
    private String apellidoMat;
    private String documentoIdentidad;
    @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
            message = "La contrasena debe tener al menos 1 mayuscula, 1 numero y 1 caracter especial")
    private String contraseña;
    @Email(message = "El formato del email no es válido")
    private String gmail;
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    private LocalDate fechaNaci;

    private String tipoContrato;
    private LocalDate fechaContratacion;
    private String especialidad;
    private String gradoAcademico;

    private Long accesoId;
}