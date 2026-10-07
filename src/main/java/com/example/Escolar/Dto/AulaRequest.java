package com.example.Escolar.Dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AulaRequest {
    // Los mismos valores que el front usa en lib/schemas/academico.ts; al ser
    // constantes pueden alimentar las anotaciones y los mensajes del error.
    public static final int CAPACIDAD_MIN = 1;
    public static final int CAPACIDAD_MAX = 50;

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(
            regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9 .-]+$",
            message = "Solo se admiten letras, números, espacios, guiones y puntos"
    )
    private String nombre;
    @NotNull(message = "La capacidad es obligatoria")
    @Min(value = CAPACIDAD_MIN, message = "La capacidad minima es " + CAPACIDAD_MIN)
    @Max(value = CAPACIDAD_MAX, message = "La capacidad maxima es " + CAPACIDAD_MAX)
    private Integer capacidad;
    private Long accesoId;
}