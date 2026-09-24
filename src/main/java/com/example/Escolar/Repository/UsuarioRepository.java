package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    List<Usuario> findByAccesoNot(Acceso acceso);

    Page<Usuario> findByAccesoNot(Acceso acceso, Pageable pageable);

    Optional<Usuario> findByIdUsuarioAndAccesoNot(Integer idUsuario, Acceso acceso);

    Optional<Usuario> findByCodigoAndAccesoNot(String codigo, Acceso acceso);

    Optional<Usuario> findByCodigo(String codigo);

    Optional<Usuario> findByDocumentoIdentidadAndAccesoNot(String documentoIdentidad, Acceso acceso);

    Optional<Usuario> findByDocumentoIdentidad(String documentoIdentidad);

    long countByCodigoStartingWith(String prefijo);

    List<Usuario> findByFechaCreacionBetweenAndAccesoNot(LocalDate inicio, LocalDate fin, Acceso acceso);

    Optional<Usuario> findByResetToken(String resetToken);

    Optional<Usuario> findByGmailAndAccesoNot(String gmail, Acceso acceso);

    boolean existsByGmailAndAccesoNot(String gmail, Acceso acceso);
}
