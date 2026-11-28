package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LoginResponse {
    private String token;
    private UsuarioResponse usuario;
    private PermisosRolResponse permisos;
    private boolean esAdmin;
    // Contraseña correcta pero falta el código del correo: no hay sesión aún.
    private boolean requiereDosFactor;
    private String desafio;
    private String correoEnmascarado;
    private Long segundosReenvio;
    private Integer minutosCodigo;
    private boolean requiereSeleccionRol;
    private String desafioSeleccionRol;
    private List<RolAsignadoSesionResponse> roles;
}
