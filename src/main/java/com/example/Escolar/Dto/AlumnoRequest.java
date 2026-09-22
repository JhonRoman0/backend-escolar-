package com.example.Escolar.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class AlumnoRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    @NotBlank(message = "El apellido paterno es obligatorio")
    private String apellidoPat;
    @NotBlank(message = "El apellido materno es obligatorio")
    private String apellidoMat;
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    private LocalDate fechaNacimiento;
    private String direccion;
    private String documentoIdentidad;

    @Valid
    @Size(max = 2, message = "Un alumno puede tener máximo 2 apoderados")
    private List<ApoderadoRequest> apoderados;

    private Long accesoId;
}