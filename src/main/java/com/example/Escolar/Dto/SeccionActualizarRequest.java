package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Cuerpo del renombrado de una seccion existente.
 *
 * <p>Solo lleva el nombre: la seccion se identifica por el idGradoSeccion del
 * path y su grado, turno y año se leen de la entidad existente, de modo que un
 * update no puede moverla de contexto.
 */
@Getter
@Setter
public class SeccionActualizarRequest {

    @NotBlank(message = "La sección es obligatoria")
    @Size(max = 50, message = "Máximo 50 caracteres")
    private String nombre;
}
