package com.example.Escolar.Repository;

import java.time.LocalDate;

public interface UsuarioVigenciaSesion {

    Integer getIdUsuario();

    LocalDate getFechaInicioVigencia();

    LocalDate getFechaFinVigencia();

    Integer getVersionToken();

    Long getAccesoId();
}
