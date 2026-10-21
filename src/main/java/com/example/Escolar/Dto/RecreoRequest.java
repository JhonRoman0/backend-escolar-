package com.example.Escolar.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

/** Crea a la vez un recreo por cada combinacion de grado, dia y franja. */
@Getter
@Setter
public class RecreoRequest {
    @NotNull(message = "Selecciona el año escolar")
    private Integer idAnio;
    @NotNull(message = "Selecciona el nivel")
    private Integer idNivel;
    @NotNull(message = "Selecciona el turno")
    private Integer idTurno;
    @NotEmpty(message = "Selecciona al menos un grado")
    private List<Integer> idGrados;
    @NotEmpty(message = "Selecciona al menos un día")
    private List<Byte> dias;
    @NotEmpty(message = "Agrega al menos un recreo")
    @Valid
    private List<Franja> franjas;

    @Getter
    @Setter
    public static class Franja {
        @NotNull(message = "La hora de inicio es obligatoria")
        private LocalTime horaInicio;
        @NotNull(message = "La hora de fin es obligatoria")
        private LocalTime horaFin;
    }
}
