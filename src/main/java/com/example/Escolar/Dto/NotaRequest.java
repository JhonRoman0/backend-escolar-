package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotaRequest {
    @NotNull(message = "La matrícula es obligatoria")
    private Integer idMatricula;
    @NotNull(message = "La competencia es obligatoria")
    private Integer idCompetencia;
    @NotNull(message = "El bimestre es obligatorio")
    private Byte bimestre;
    @NotNull(message = "La calificación es obligatoria")
    @Pattern(regexp = "^(AD|A|B|C)$", message = "La calificación debe ser AD, A, B o C")
    private String calificacion;
    private String conclusionDescriptiva;
    private String codigoAutorizacion;
}
