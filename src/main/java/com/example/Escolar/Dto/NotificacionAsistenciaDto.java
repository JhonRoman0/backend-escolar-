package com.example.Escolar.Dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record NotificacionAsistenciaDto(String alumnoNombre,
                                        LocalDate fecha,
                                        LocalTime horaEntrada,
                                        String estado,
                                        List<String> celulares) {
}