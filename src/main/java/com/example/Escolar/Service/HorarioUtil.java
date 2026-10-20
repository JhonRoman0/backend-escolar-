package com.example.Escolar.Service;

import com.example.Escolar.Model.Turno;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Reglas de horas y dias que comparten recreos y asignaciones. */
final class HorarioUtil {

    private static final String[] DIAS = {"", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado"};
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private HorarioUtil() {
    }

    /** De lunes (1) a sabado (6): el domingo no hay clases. */
    static void validarDia(Byte dia) {
        if (dia == null || dia < 1 || dia > 6) {
            throw new IllegalArgumentException("El día debe ser de lunes a sábado");
        }
    }

    static String dia(byte dia) {
        return dia >= 1 && dia <= 6 ? DIAS[dia] : String.valueOf(dia);
    }

    static String hora(LocalTime hora) {
        return hora.format(HORA);
    }

    static String rango(LocalTime inicio, LocalTime fin) {
        return hora(inicio) + "–" + hora(fin);
    }

    /** Fin posterior al inicio y todo dentro del horario del turno. */
    static void validarDentroDelTurno(LocalTime inicio, LocalTime fin, Turno turno) {
        if (!inicio.isBefore(fin)) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio ("
                    + rango(inicio, fin) + ")");
        }
        if (inicio.isBefore(turno.getHoraEntrada()) || fin.isAfter(turno.getHoraSalida())) {
            throw new IllegalArgumentException("El horario " + rango(inicio, fin) + " está fuera del turno "
                    + turno.getNombre() + " (" + rango(turno.getHoraEntrada(), turno.getHoraSalida()) + ")");
        }
    }

    /** Se tocan en el borde (una termina 10:00 y la otra empieza 10:00) sin cruzarse. */
    static boolean seCruzan(LocalTime inicioA, LocalTime finA, LocalTime inicioB, LocalTime finB) {
        return inicioA.isBefore(finB) && inicioB.isBefore(finA);
    }
}
