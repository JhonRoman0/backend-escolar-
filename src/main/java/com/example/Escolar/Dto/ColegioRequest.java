package com.example.Escolar.Dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ColegioRequest {
    @NotBlank(message = "El nombre del colegio es obligatorio")
    private String nombre;
    private String celular;
    private String telefono;
    private String direccion;
    private String codigoColegio;
    private String urlFoto;
    private String urlPortal;
    private Long accesoId;
}