package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class NotaResponse {
    private Integer idNota;
    private Integer idMatricula;
    private Integer idAlumno;
    private String codigoAlumno;
    private String alumno;
    private Integer idCompetencia;
    private String competencia;
    private Integer idCurso;
    private String curso;
    private Byte bimestre;
    private String calificacion;
    private String conclusionDescriptiva;
    private LocalDate fechaRegistro;
    private String docenteRegistro;
    private Byte acceso;
}
