package com.example.Escolar.Dto;

import com.example.Escolar.Model.Sexo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class InscripcionAlumnoRequest {
    @NotBlank(message = "El DNI del alumno es obligatorio")
    @Pattern(regexp = "^[0-9]{8}$", message = "El DNI del alumno debe tener 8 dígitos")
    private String documentoIdentidad;
    @NotBlank(message = "El nombre del alumno es obligatorio")
    private String nombre;
    @NotBlank(message = "El apellido paterno del alumno es obligatorio")
    private String apellidoPat;
    @NotBlank(message = "El apellido materno del alumno es obligatorio")
    private String apellidoMat;
    @NotNull(message = "La fecha de nacimiento del alumno es obligatoria")
    private LocalDate fechaNacimiento;
    @NotNull(message = "El sexo del alumno es obligatorio")
    private Sexo sexo;
    @NotBlank(message = "La dirección del alumno es obligatoria")
    @Size(max = 120, message = "La dirección no puede pasar de 120 caracteres")
    private String direccion;
    private boolean tieneDiscapacidad;
    @Size(max = 255, message = "La discapacidad no puede pasar de 255 caracteres")
    private String discapacidad;
}
