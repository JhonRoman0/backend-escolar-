package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotNull;
import com.example.Escolar.Model.EscalaCalificacion;
import com.example.Escolar.Model.TipoPeriodo;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ConfiguracionAcademicaRequest {
    @NotNull(message = "Selecciona el año escolar")
    private Integer idAnio;
    @NotNull(message = "Selecciona el nivel")
    private Integer idNivel;
    /** De 30 a 60, de 5 en 5. Null: libre. */
    private Integer minutosHoraAcademica;
    /** Null: se mantiene la actual. */
    private EscalaCalificacion escala;
    /** Null: se mantiene el actual. */
    private TipoPeriodo tipoPeriodo;
    /** En orden. Null: se mantienen los guardados. */
    private List<@Valid PeriodoRequest> periodos;

    public record PeriodoRequest(@NotNull(message = "Indica la fecha de inicio de cada periodo") LocalDate fechaInicio,
                                 @NotNull(message = "Indica la fecha de fin de cada periodo") LocalDate fechaFin) {
    }
}
