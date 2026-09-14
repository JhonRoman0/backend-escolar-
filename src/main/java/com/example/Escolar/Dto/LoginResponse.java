package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private String token;
    private UsuarioResponse usuario;
    private PermisosRolResponse permisos;
    private boolean esAdmin;
}
