package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfiguracionAcademicaRequest {
    @NotNull(message = "Selecciona el año escolar")
    private Integer idAnio;
    @NotNull(message = "Selecciona el nivel")
    private Integer idNivel;
    /** De 30 a 60, de 5 en 5. Null: libre. */
    private Integer minutosHoraAcademica;
}
