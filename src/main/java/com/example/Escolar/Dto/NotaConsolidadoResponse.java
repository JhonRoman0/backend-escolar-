package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class NotaConsolidadoResponse {
    private Integer idMatricula;
    private Integer idAlumno;
    private String codigoAlumno;
    private String alumno;
    private Integer idCurso;
    private String curso;
    private Byte bimestre;
    private List<CompetenciaNota> competencias;

    @Getter
    @Setter
    public static class CompetenciaNota {
        private Integer idCompetencia;
        private String competencia;
        private Byte orden;
        private Integer idNota;
        private String calificacion;
        private String conclusionDescriptiva;
        private LocalDate fechaRegistro;
    }
}
