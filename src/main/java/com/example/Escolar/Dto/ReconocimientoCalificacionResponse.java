package com.example.Escolar.Dto;
import java.time.LocalDateTime;
public record ReconocimientoCalificacionResponse(Integer idReconocimiento, Integer idEvaluacionDestino,
        String evaluacionDestino, String seccionDestino, String calificacion, String estado,
        String usuario, LocalDateTime fecha, String motivoAnulacion, String usuarioAnulacion,
        LocalDateTime fechaAnulacion, boolean puedeAnular, boolean enSeccionActual) {
    public record Candidata(Integer idEvaluacion, String nombre, String bloqueo) {}
    public record Procedencia(Integer idReconocimiento, String seccion, String evaluacion) {}
}
