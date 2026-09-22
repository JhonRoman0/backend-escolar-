package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AsignacionResponse {
    private Integer idAsignacion;
    private Integer idCurso;
    private String curso;
    private Integer idDocente;
    private String docente;
    private Integer idGradoSeccion;
    private String grado;
    private String seccion;
    private String turno;
    private Integer idAnio;
    private String anio;
    private Long accesoId;
    private List<HorarioResponse> horarios;
}