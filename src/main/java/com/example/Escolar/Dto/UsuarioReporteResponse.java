package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class UsuarioReporteResponse {
    private Integer idUsuario;
    private String nombre;
    private String codigo;
    private String documentoIdentidad;
    private String gmail;
    private LocalDate fechaCreacion;
    private List<String> roles;
}
