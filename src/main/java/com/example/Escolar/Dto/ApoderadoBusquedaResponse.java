package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/** Resultado de buscar un apoderado por DNI en el paso 2. */
@Getter
@Setter
public class ApoderadoBusquedaResponse {
    /** APODERADO: ya es apoderado; USUARIO: tiene cuenta con otro rol; NUEVO: no está en el sistema. */
    private String estado;
    private Integer idApoderado;
    private String documentoIdentidad;
    private String nombre;
    private String apellidoPat;
    private String apellidoMat;
    private LocalDate fechaNaci;
    private String sexo;
    private String direccion;
    private String celular;
    private String gmail;
    /** Parentesco con el alumno que se está matriculando, si ya estaban vinculados. */
    private Integer idParentesco;
    private List<String> hijos;
}
