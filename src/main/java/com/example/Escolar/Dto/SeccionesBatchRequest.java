package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Alta de varias secciones de una vez sobre un grado que ya existe.
 *
 * <p>Todas comparten la misma combinacion de grado, turno y anio: es exactamente
 * el caso que resuelve la pantalla "Nueva seccion", donde el usuario elige esa
 * combinacion una sola vez y escribe A, B, C.
 *
 * <p>El lote se valida entero antes de insertar, asi que si una sola letra esta
 * repetida o ya existe no se crea ninguna.
 */
@Getter
@Setter
public class SeccionesBatchRequest {

    @NotNull(message = "El grado es obligatorio")
    private Integer idGrado;

    @NotNull(message = "El turno es obligatorio")
    private Integer idTurno;

    /**
     * Por lo menos una y hasta 26: una letra por seccion. El limite es generouso
     * a proposito, el frontend muestra un input por letra y el alphabeto no
     * alcanza para un lote util mas grande que ese.
     */
    @NotEmpty(message = "Indica al menos una sección")
    @Size(max = 26, message = "Máximo 26 secciones por lote")
    private List<@NotBlank(message = "La sección es obligatoria") @Size(max = 50, message = "Máximo 50 caracteres") String> nombres;

    /**
     * Opcional a proposito, igual que en {@link SeccionRequest}: si no viene se
     * usa el anio vigente. Mandarlo desde el formulario solo abriria la puerta a
     * colgar las secciones de un anio que el usuario no esta viendo.
     */
    private Integer idAnio;
}