package com.example.Escolar.Repository;

import com.example.Escolar.Model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    List<Usuario> findByAccesoNot(Byte acceso);

    Page<Usuario> findByAccesoNot(Byte acceso, Pageable pageable);

    Optional<Usuario> findByIdUsuarioAndAccesoNot(Integer idUsuario, Byte acceso);

    Optional<Usuario> findByCodigoAndAccesoNot(String codigo, Byte acceso);

    Optional<Usuario> findByCodigo(String codigo);

    Optional<Usuario> findByDocumentoIdentidadAndAccesoNot(String documentoIdentidad, Byte acceso);

    long countByCodigoStartingWith(String prefijo);

    List<Usuario> findByFechaCreacionBetweenAndAccesoNot(LocalDate inicio, LocalDate fin, Byte acceso);
}
