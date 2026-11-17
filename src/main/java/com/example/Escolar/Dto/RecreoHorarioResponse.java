package com.example.Escolar.Dto;

import java.time.LocalTime;

/** Franja de recreo para pintar en un horario. */
public record RecreoHorarioResponse(Integer idTurno, String turno, Byte diaSemana, LocalTime horaInicio,
                                    LocalTime horaFin, String grado) {
}
