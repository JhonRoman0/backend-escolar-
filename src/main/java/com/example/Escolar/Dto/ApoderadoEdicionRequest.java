package com.example.Escolar.Dto;

import com.example.Escolar.Model.Sexo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Edición desde Estudiantes: todo obligatorio. El parentesco es el del alumno desde el que se edita. */
@Getter
@Setter
public class ApoderadoEdicionRequest {
    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "^[0-9]{8}$", message = "El DNI debe tener 8 dígitos")
    private String documentoIdentidad;
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    @NotBlank(message = "El apellido paterno es obligatorio")
    private String apellidoPat;
    @NotBlank(message = "El apellido materno es obligatorio")
    private String apellidoMat;
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    private LocalDate fechaNaci;
    @NotNull(message = "El sexo es obligatorio")
    private Sexo sexo;
    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 120, message = "La dirección no puede pasar de 120 caracteres")
    private String direccion;
    @NotBlank(message = "El celular es obligatorio")
    @Pattern(regexp = "^[0-9]{9}$", message = "El celular debe tener 9 dígitos")
    private String celular;
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es válido")
    private String gmail;
    @NotNull(message = "El alumno es obligatorio")
    private Integer idAlumno;
    @NotNull(message = "El parentesco es obligatorio")
    private Integer idParentesco;
}
