package com.example.Escolar.Dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactoActualizarRequest {
    @Min(value = 1, message = "estado debe ser 1 (Pendiente), 2 (Leído) o 3 (Contestado)")
    @Max(value = 3, message = "estado debe ser 1 (Pendiente), 2 (Leído) o 3 (Contestado)")
    private Byte estado;
}