package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MetodoPagoRequest {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 40, message = "El nombre no puede pasar de 40 caracteres")
    private String nombre;
    private boolean requiereCodigo;
    private boolean requiereComprobante;
    /** null al crear: activo. */
    private Boolean activo;
}
