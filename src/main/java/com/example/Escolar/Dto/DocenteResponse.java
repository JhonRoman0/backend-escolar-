package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class DocenteResponse {
    private Integer idDocente;
    private Integer idUsuario;
    private String codigo;
    private String nombre;
    private String apellidoPat;
    private String apellidoMat;
    private String documentoIdentidad;
    private String gmail;
    private LocalDate fechaNaci;
    private String urlFoto;
    private byte acceso;
    private String tipoContrato;
    private LocalDate fechaContratacion;
    private String especialidad;
    private String gradoAcademico;
    private List<RolResponse> roles;
}