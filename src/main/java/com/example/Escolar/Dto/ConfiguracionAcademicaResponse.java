package com.example.Escolar.Dto;

import com.example.Escolar.Model.EscalaCalificacion;
import com.example.Escolar.Model.TipoPeriodo;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ConfiguracionAcademicaResponse {
    private Integer idAnio;
    private String anio;
    private Integer idNivel;
    private String nivel;
    /** Minutos de la hora academica; null: libre. */
    private Integer minutosHoraAcademica;
    /** CONFIGURADO (guardado en este año), HEREDADO (de un año anterior) o DEFECTO. */
    private String origen;
    /** Año del que se heredo, si origen es HEREDADO. */
    private String anioOrigen;
    /** Asignaciones del año en el nivel: con alguna, la duracion ya no cambia. */
    private long asignaciones;
    private EscalaCalificacion escala;
    private TipoPeriodo tipoPeriodo;
    private List<PeriodoEvaluacionResponse> periodos;
    /** Si se puede guardar, y si no, por qué. */
    private boolean editable;
    private String motivoBloqueo;
}
