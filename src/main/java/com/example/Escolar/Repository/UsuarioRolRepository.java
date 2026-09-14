package com.example.Escolar.Repository;

import com.example.Escolar.Model.UsuarioRol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, Integer> {
    List<UsuarioRol> findByRolIdRol(Integer idRol);

    List<UsuarioRol> findByUsuarioIdUsuario(Integer idUsuario);

    void deleteByUsuarioIdUsuario(Integer idUsuario);
}
