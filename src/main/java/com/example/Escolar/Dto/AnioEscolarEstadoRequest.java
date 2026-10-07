package com.example.Escolar.Dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// Solo lleva el estado. Separlo del request completo evita que una transición
// arrastre fechas o nombre por accidente y los pise sin querer.
@Getter
@Setter
public class AnioEscolarEstadoRequest {
    @NotNull(message = "El estado es obligatorio")
    @Min(value = 1, message = "El estado debe ser 1 (vigente), 2 (cerrado) o 3 (por comenzar)")
    @Max(value = 3, message = "El estado debe ser 1 (vigente), 2 (cerrado) o 3 (por comenzar)")
    private Byte estado;
}