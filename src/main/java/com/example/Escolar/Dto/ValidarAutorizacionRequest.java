package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidarAutorizacionRequest {
    @NotBlank(message = "El código de autorización es obligatorio")
    private String codigo;
}