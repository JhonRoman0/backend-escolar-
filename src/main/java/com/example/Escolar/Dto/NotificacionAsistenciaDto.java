package com.example.Escolar.Dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Aviso de asistencia a los apoderados. tipo: INGRESO (puntual, tardanza o
 * justificado), FALTA o JUSTIFICACION (de una marca ya registrada).
 */
public record NotificacionAsistenciaDto(Tipo tipo,
                                        String alumnoNombre,
                                        LocalDate fecha,
                                        LocalTime horaEntrada,
                                        String estado,
                                        String motivo,
                                        String acompanante,
                                        List<String> celulares,
                                        List<String> correos) {
    public enum Tipo { INGRESO, FALTA, JUSTIFICACION }
}
