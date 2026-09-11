package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NotaBatchRequest {
    @NotNull(message = "La competencia es obligatoria")
    private Integer idCompetencia;
    @NotNull(message = "El bimestre es obligatorio")
    private Byte bimestre;
    @NotEmpty(message = "Debe incluir al menos una nota")
    private List<Item> notas;
    private String codigoAutorizacion;

    @Getter
    @Setter
    public static class Item {
        @NotNull(message = "La matrícula es obligatoria")
        private Integer idMatricula;
        @NotNull(message = "La calificación es obligatoria")
        @Pattern(regexp = "^(AD|A|B|C)$", message = "La calificación debe ser AD, A, B o C")
        private String calificacion;
        private String conclusionDescriptiva;
    }
}
