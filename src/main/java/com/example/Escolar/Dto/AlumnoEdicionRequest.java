package com.example.Escolar.Dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlumnoEdicionRequest {
    private boolean tieneDiscapacidad;
    @Size(max = 255, message = "La discapacidad no puede pasar de 255 caracteres")
    private String discapacidad;
}
