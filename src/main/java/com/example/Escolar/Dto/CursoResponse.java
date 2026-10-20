package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CursoResponse {
    private Integer idCurso;
    private String codigo;
    private String nombre;
    private Long accesoId;
    private Integer idAnio;
    private String anio;
    private Integer idNivel;
    private String nivel;
    /** AREA, TRANSVERSAL o SUBCURSO. */
    private String tipo;
    private boolean tieneSubcursos;
    private Integer idCursoPadre;
    /** Competencias activas del area o transversal. */
    private int cantidadCompetencias;
    /** Solo subcursos: competencias del area que evalua. */
    private List<Integer> idCompetencias;
}
