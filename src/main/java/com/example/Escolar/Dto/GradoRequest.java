package com.example.Escolar.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GradoRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    @NotNull(message = "El nivel es obligatorio")
    private Integer idNivel;

    /**
     * Opcional a proposito. El ano de una seccion no lo elige quien escribe: si no
     * viene se usa el vigente, y al editar se conserva el que ya tenia. Mandarlo
     * desde el formulario solo permitiria mover secciones de ano sin querer.
     */
    private Integer idAnio;

    @NotNull(message = "El turno es obligatorio")
    private Integer idTurno;
    /** Secciones con su salon. Al editar, una existente se reconoce por su nombre. */
    @Valid
    private List<SeccionNueva> secciones;
    /** Salon de la seccion unica, cuando el grado no tiene secciones. */
    private Integer idAula;
    private Long accesoId;
}