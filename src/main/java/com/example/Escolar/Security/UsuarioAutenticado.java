package com.example.Escolar.Security;

import java.util.List;

public record UsuarioAutenticado(Integer idUsuario, String codigo, List<String> roles, Integer idDispositivo) {
    public UsuarioAutenticado(Integer idUsuario, String codigo, List<String> roles) {
        this(idUsuario, codigo, roles, null);
    }
}
