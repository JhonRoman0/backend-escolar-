package com.example.Escolar.Security;

import java.util.List;

public record UsuarioAutenticado(Integer idUsuario, String codigo, List<String> roles, Integer idDispositivo,
                                 Integer idRolActivo, Integer versionToken) {
    public UsuarioAutenticado(Integer idUsuario, String codigo, List<String> roles, Integer idDispositivo,
                              Integer idRolActivo) {
        this(idUsuario, codigo, roles, idDispositivo, idRolActivo, null);
    }

    public UsuarioAutenticado(Integer idUsuario, String codigo, List<String> roles, Integer idDispositivo) {
        this(idUsuario, codigo, roles, idDispositivo, null, null);
    }

    public UsuarioAutenticado(Integer idUsuario, String codigo, List<String> roles) {
        this(idUsuario, codigo, roles, null, null, null);
    }
}
