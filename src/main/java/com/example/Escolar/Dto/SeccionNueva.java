package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Una seccion con su salon, tal como llega al crear un grado o un lote. */
@Getter
@Setter
public class SeccionNueva {
    @NotBlank(message = "La sección es obligatoria")
    @Size(max = 50, message = "Máximo 50 caracteres")
    private String nombre;
    @NotNull(message = "Elige el salón de la sección")
    private Integer idAula;
}
