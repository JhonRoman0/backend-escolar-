package com.example.Escolar.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class UsuarioRequest {
    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "Solo se permiten letras y espacios")
    private String nombre;
    @NotBlank(message = "El apellido paterno es obligatorio")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "Solo se permiten letras y espacios")
    private String apellidoPat;
    @NotBlank(message = "El apellido materno es obligatorio")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "Solo se permiten letras y espacios")
    private String apellidoMat;
    @NotBlank(message = "El documento de identidad es obligatorio")
    @Size(min = 8, max = 8, message = "El DNI debe contener exactamente 8 digitos")
    @Pattern(regexp = "^[0-9]+$", message = "El DNI debe contener solo digitos")
    private String documentoIdentidad;
    @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
            message = "La contrasena debe tener al menos 1 mayuscula, 1 numero y 1 caracter especial")
    private String contraseña;
    private Long accesoId;
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    private String gmail;
    @NotBlank(message = "El celular es obligatorio")
    @Pattern(regexp = "^[0-9]{9}$", message = "El celular debe contener 9 digitos")
    private String celular;
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    private LocalDate fechaNaci;
    private String urlFoto;
    private String pkUrlFoto;
    private List<Integer> rolIds;

    // Datos especificos del docente (solo se usan cuando el rol DOCENTE esta seleccionado).
    private Integer gradoAcademicoId;
    private Integer tipoContratoId;
    private LocalDate fechaContratacion;
    private List<Integer> niveles;
}
