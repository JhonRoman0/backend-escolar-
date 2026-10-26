package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MontoMatriculaResponse {
    private Integer idNivel;
    private String nivel;
    /** null: aún no configurado para ese año. */
    private BigDecimal monto;
    /** Monto del año anterior cuando este aún no tiene: el formulario lo propone. */
    private BigDecimal sugerido;
}
