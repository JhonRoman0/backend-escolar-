package com.example.Escolar.Dto;

import java.time.LocalDateTime;

public record ReconocimientoCalificacionResponse(Integer idReconocimiento, Integer idEvaluacionDestino,
        String evaluacionDestino, String seccionDestino, String calificacion, String estado,
        String usuario, LocalDateTime fecha, String motivoAnulacion, String usuarioAnulacion,
        LocalDateTime fechaAnulacion, boolean puedeAnular, boolean enSeccionActual) {

    /** De dónde viene una nota usada por un alumno trasladado. */
    public record Procedencia(Integer idReconocimiento, String seccion, String evaluacion) {
    }
}
