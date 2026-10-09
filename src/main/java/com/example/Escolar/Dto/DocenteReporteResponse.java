package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class DocenteReporteResponse {
    private Integer idDocente;
    private String nombre;
    private String codigo;
    private String gradoAcademico;
    private String tipoContrato;
    private List<String> niveles;
    private LocalDate fechaContratacion;
    private String documentoIdentidad;
}
