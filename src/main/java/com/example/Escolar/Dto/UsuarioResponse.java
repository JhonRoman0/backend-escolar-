package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class UsuarioResponse {
    private Integer idUsuario;
    private String nombre;
    private String apellidoPat;
    private String apellidoMat;
    private String codigo;
    private String documentoIdentidad;
    private byte acceso;
    private String gmail;
    private LocalDate fechaNaci;
    private String urlFoto;
    private String pkUrlFoto;
    private String nombreRol;
    private List<RolResponse> roles;
    private Integer intentosFallidos;
    private LocalDateTime fechaBloqueo;
}
