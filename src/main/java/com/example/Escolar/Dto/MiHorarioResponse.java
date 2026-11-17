package com.example.Escolar.Dto;

import java.util.List;

/**
 * Horario de un docente: sus clases y, por turno, el recreo cuando en ese turno
 * dicta en un solo grado (con varios grados cada uno tiene el suyo y no se muestra).
 */
public record MiHorarioResponse(List<HorarioPlanoResponse> clases, List<RecreoHorarioResponse> recreos) {
}
