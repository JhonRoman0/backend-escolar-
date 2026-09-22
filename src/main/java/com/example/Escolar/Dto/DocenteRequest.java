package com.example.Escolar.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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