package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PromedioResponse {
    private Integer idMatricula;
    private Integer idAlumno;
    private String codigoAlumno;
    private String alumno;
    private List<PorBimestre> promedios;

    @Getter
    @Setter
    public static class PorBimestre {
        private Byte bimestre;
        private String promedioLiteral;
    }
}
