package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DocenteReporteResponse {
    private Integer idDocente;
    private String nombre;
    private String codigo;
    private String especialidad;
    private String gradoAcademico;
    private String tipoContrato;
    private LocalDate fechaContratacion;
    private String documentoIdentidad;
}
