package com.example.Escolar.Dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ColegioRequest {
    @NotBlank(message = "El nombre del colegio es obligatorio")
    @Size(max = 120, message = "El nombre no puede pasar de 120 caracteres")
    private String nombre;
    @NotBlank(message = "El celular es obligatorio")
    @Pattern(regexp = "^[0-9]{9}$", message = "El celular debe tener 9 dígitos")
    private String celular;
    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{6,15}$", message = "El teléfono debe tener entre 6 y 15 dígitos")
    private String telefono;
    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 150, message = "La dirección no puede pasar de 150 caracteres")
    private String direccion;
    @Size(max = 20, message = "El código no puede pasar de 20 caracteres")
    private String codigoColegio;
    private Long accesoId;
}