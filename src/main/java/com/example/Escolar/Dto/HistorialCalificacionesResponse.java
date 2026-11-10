package com.example.Escolar.Dto;
import java.time.*;
import java.util.List;
public record HistorialCalificacionesResponse(Integer idNotaOrigen, String seccion, String curso,
        String area, String evaluacion, List<EvaluacionResponse.Competencia> competencias,
        int periodo, LocalDate fecha, String calificacion, String bloqueo,
        List<ReconocimientoCalificacionResponse> reconocimientos) {}
