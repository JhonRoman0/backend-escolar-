package com.example.Escolar.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class MontoMatriculaRequest {
    @NotNull(message = "El año escolar es obligatorio")
    private Integer idAnio;
    @NotEmpty(message = "Indica al menos un monto")
    @Valid
    private List<Item> montos;

    @Getter
    @Setter
    public static class Item {
        @NotNull(message = "El nivel es obligatorio")
        private Integer idNivel;
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        private BigDecimal monto;
    }
}
