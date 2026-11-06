package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompetenciaResponse {
    private Integer idCompetencia;
    private String codigo;
    private String competencia;
    private Integer idCurso;
    private String curso;
    private int orden;
    private Long accesoId;
}
