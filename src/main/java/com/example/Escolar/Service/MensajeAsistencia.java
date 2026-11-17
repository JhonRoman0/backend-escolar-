package com.example.Escolar.Service;

import com.example.Escolar.Dto.NotificacionAsistenciaDto;

import java.time.format.DateTimeFormatter;

/** El texto del aviso de asistencia, igual para correo, SMS y simulador. */
final class MensajeAsistencia {
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private MensajeAsistencia() {
    }

    static String asunto(NotificacionAsistenciaDto e) {
        return switch (e.tipo()) {
            case INGRESO -> "Ingreso de " + e.alumnoNombre() + " (" + e.estado().toLowerCase() + ")";
            case FALTA -> e.alumnoNombre() + " no registró su ingreso hoy";
            case JUSTIFICACION -> "Asistencia justificada de " + e.alumnoNombre();
        };
    }

    static String texto(NotificacionAsistenciaDto e) {
        String fecha = e.fecha().format(FECHA);
        return switch (e.tipo()) {
            case INGRESO -> e.alumnoNombre() + " registró su ingreso el " + fecha + " a las " + e.horaEntrada().format(HORA)
                    + " (" + e.estado().toLowerCase() + ")."
                    + (e.acompanante() != null ? " Ingresó acompañado de " + e.acompanante() + "." : "")
                    + (e.motivo() != null ? " Motivo: " + e.motivo() + "." : "");
            case FALTA -> e.alumnoNombre() + " no registró su ingreso al colegio el " + fecha
                    + ". Si llega más tarde, ingresará con justificación.";
            case JUSTIFICACION -> "La asistencia de " + e.alumnoNombre() + " del " + fecha + " fue justificada"
                    + (e.motivo() != null ? ": " + e.motivo() : "") + ".";
        };
    }
}
