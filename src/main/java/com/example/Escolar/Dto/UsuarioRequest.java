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
    private String nombre;
    @NotBlank(message = "El apellido paterno es obligatorio")
    private String apellidoPat;
    @NotBlank(message = "El apellido materno es obligatorio")
    private String apellidoMat;
    @NotBlank(message = "El documento de identidad es obligatorio")
    @Size(min = 8, max = 8, message = "El DNI debe contener exactamente 8 digitos")
    @Pattern(regexp = "^[0-9]+$", message = "El DNI debe contener solo digitos")
    private String documentoIdentidad;
    private String contraseña;
    private Long accesoId;
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    private String gmail;
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    private LocalDate fechaNaci;
    private String urlFoto;
    private String pkUrlFoto;
    private List<Integer> rolIds;
}
