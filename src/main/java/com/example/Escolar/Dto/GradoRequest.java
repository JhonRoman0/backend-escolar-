package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GradoRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    @NotNull(message = "El nivel es obligatorio")
    private Integer idNivel;
    @NotNull(message = "El año es obligatorio")
    private Integer idAnio;
    @NotNull(message = "El turno es obligatorio")
    private Integer idTurno;
    private List<@NotBlank(message = "El nombre de la sección es obligatorio") String> secciones;
    private Byte acceso;
}