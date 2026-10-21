package com.example.Escolar.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SubcursoRequest {
    @NotBlank(message = "El nombre del subcurso es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;
    @NotEmpty(message = "Selecciona al menos una competencia")
    private List<Integer> idCompetencias;
}
