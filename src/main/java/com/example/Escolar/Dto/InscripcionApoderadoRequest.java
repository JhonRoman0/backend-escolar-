package com.example.Escolar.Dto;

import com.example.Escolar.Model.Sexo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Apoderado del paso 2. Si el DNI ya es apoderado solo se usa el parentesco; si
 * ya es usuario con otro rol no se piden correo ni contraseña. Por eso la
 * obligatoriedad de los demás campos la decide el servicio.
 */
@Getter
@Setter
public class InscripcionApoderadoRequest {
    @NotBlank(message = "El DNI del apoderado es obligatorio")
    @Pattern(regexp = "^[0-9]{8}$", message = "El DNI del apoderado debe tener 8 dígitos")
    private String documentoIdentidad;
    private String nombre;
    private String apellidoPat;
    private String apellidoMat;
    private LocalDate fechaNaci;
    private Sexo sexo;
    @Size(max = 120, message = "La dirección no puede pasar de 120 caracteres")
    private String direccion;
    private Integer idParentesco;
    @Pattern(regexp = "^[0-9]{9}$", message = "El celular debe tener 9 dígitos")
    private String celular;
    @Email(message = "El correo del apoderado no es válido")
    private String gmail;
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
            message = "La contraseña debe tener al menos 8 caracteres, 1 mayúscula, 1 número y 1 carácter especial")
    private String contraseña;
}
