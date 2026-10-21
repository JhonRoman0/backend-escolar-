package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Lo que se leyo del Excel y lo que pasaria al importarlo, sin guardar nada. */
@Getter
@Setter
public class CursoVistaPreviaResponse {
    private Integer idNivel;
    private String nivel;
    /** Datos tal como vienen en la hoja Generalidades. */
    private String nivelArchivo;
    private String anioArchivo;
    private String periodoArchivo;
    private List<Area> areas;
    /** Nombres de las areas del año destino que no vienen en el archivo. */
    private List<String> seInactivan;
    private List<String> advertencias;

    @Getter
    @Setter
    public static class Area {
        private String codigo;
        private String nombre;
        private String tipo;
        /** NUEVA o ACTUALIZA. */
        private String accion;
        private List<Competencia> competencias;
    }

    @Getter
    @Setter
    public static class Competencia {
        private String codigo;
        private String nombre;
    }
}
