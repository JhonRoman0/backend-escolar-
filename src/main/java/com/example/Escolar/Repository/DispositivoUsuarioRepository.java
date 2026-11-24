package com.example.Escolar.Repository;

import com.example.Escolar.Model.DispositivoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DispositivoUsuarioRepository extends JpaRepository<DispositivoUsuario, Integer> {
    Optional<DispositivoUsuario> findByUsuarioIdUsuarioAndTokenHash(Integer idUsuario, String tokenHash);

    Optional<DispositivoUsuario> findByIdDispositivoAndUsuarioIdUsuario(Integer idDispositivo, Integer idUsuario);

    List<DispositivoUsuario> findByUsuarioIdUsuarioOrderByUltimoUsoDesc(Integer idUsuario);

    boolean existsByUsuarioIdUsuario(Integer idUsuario);
}
