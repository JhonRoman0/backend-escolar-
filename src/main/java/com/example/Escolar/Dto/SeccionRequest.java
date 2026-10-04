package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeccionRequest {

    @NotNull(message = "El grado es obligatorio")
    private Integer idGrado;

    @NotNull(message = "El turno es obligatorio")
    private Integer idTurno;

    @NotBlank(message = "La sección es obligatoria")
    @Size(max = 50, message = "Máximo 50 caracteres")
    private String nombre;

    /**
     * Opcional a proposito. Al crear se usa el anio vigente y al editar se
     * conserva el que ya tenia la seccion, asi que mandarlo desde el formulario
     * solo abriria la puerta a mover una seccion de ano por error.
     */
    private Integer idAnio;
}