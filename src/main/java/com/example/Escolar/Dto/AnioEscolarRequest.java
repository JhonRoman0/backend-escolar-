package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AnioEscolarRequest {
    @NotBlank(message = "El año es obligatorio")
    // El backend no parseaba el año hasta que validarVentanaDeAnio empezó a
    // hacerlo: un "abc" sin este Pattern llegaba a Integer.parseInt y devolvía
    // 500 en vez de 400.
    @Pattern(regexp = "\\d{4}", message = "Año inválido (4 dígitos)")
    private String anio;
    @Min(value = 1, message = "El estado debe ser 1 (vigente), 2 (cerrado) o 3 (por comenzar)")
    @Max(value = 3, message = "El estado debe ser 1 (vigente), 2 (cerrado) o 3 (por comenzar)")
    private Byte estado;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Boolean bloqueoHorariosPorFecha;
    private Long accesoId;
}