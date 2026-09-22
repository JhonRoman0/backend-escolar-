package com.example.Escolar.Dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AsignacionRequest {
    @NotNull(message = "El curso es obligatorio")
    private Integer idCurso;
    @NotNull(message = "El docente es obligatorio")
    private Integer idDocente;
    @NotNull(message = "El grado-sección es obligatorio")
    private Integer idGradoSeccion;
    @NotNull(message = "El año escolar es obligatorio")
    private Integer idAnio;
    @NotEmpty(message = "Debe indicar al menos un horario")
    private List<HorarioRequest> horarios;
    private Long accesoId;
}