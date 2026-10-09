package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    List<Usuario> findByAccesoNot(Acceso acceso);

    Page<Usuario> findByAccesoNot(Acceso acceso, Pageable pageable);

    /**
     * Listado paginado con filtros opcionales. `q` ya debe venir normalizado
     * como patron LIKE (minimo '%' + texto + '%'). El filtro por rol usa EXISTS
     * sobre la tabla intermedia para incluir usuarios con ese rol aunque tengan
     * otros mas. El filtro de estado compara acceso.idAcceso y el listado
     * siempre excluye el acceso ELIMINADO.
     */
    @Query(value = "SELECT u FROM Usuario u " +
            "WHERE u.acceso.idAcceso <> :eliminadoId " +
            "AND (:inicio IS NULL OR u.fechaCreacion >= :inicio) " +
            "AND (:fin IS NULL OR u.fechaCreacion <= :fin) " +
            "AND (:q IS NULL OR LOWER(u.nombre) LIKE :q OR LOWER(u.apellidoPat) LIKE :q " +
            "OR LOWER(u.apellidoMat) LIKE :q OR LOWER(u.codigo) LIKE :q " +
            "OR LOWER(u.documentoIdentidad) LIKE :q OR LOWER(u.gmail) LIKE :q) " +
            "AND (:idRol IS NULL OR EXISTS (SELECT 1 FROM UsuarioRol ur WHERE ur.usuario.idUsuario = u.idUsuario " +
            "AND ur.rol.idRol = :idRol AND ur.rol.acceso.idAcceso <> :eliminadoId)) " +
            "AND (:idAcceso IS NULL OR u.acceso.idAcceso = :idAcceso)",
            countQuery = "SELECT COUNT(u) FROM Usuario u " +
            "WHERE u.acceso.idAcceso <> :eliminadoId " +
            "AND (:inicio IS NULL OR u.fechaCreacion >= :inicio) " +
            "AND (:fin IS NULL OR u.fechaCreacion <= :fin) " +
            "AND (:q IS NULL OR LOWER(u.nombre) LIKE :q OR LOWER(u.apellidoPat) LIKE :q " +
            "OR LOWER(u.apellidoMat) LIKE :q OR LOWER(u.codigo) LIKE :q " +
            "OR LOWER(u.documentoIdentidad) LIKE :q OR LOWER(u.gmail) LIKE :q) " +
            "AND (:idRol IS NULL OR EXISTS (SELECT 1 FROM UsuarioRol ur WHERE ur.usuario.idUsuario = u.idUsuario " +
            "AND ur.rol.idRol = :idRol AND ur.rol.acceso.idAcceso <> :eliminadoId)) " +
            "AND (:idAcceso IS NULL OR u.acceso.idAcceso = :idAcceso)")
    Page<Usuario> buscarFiltrados(
            @Param("eliminadoId") Long eliminadoId,
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin,
            @Param("q") String q,
            @Param("idRol") Integer idRol,
            @Param("idAcceso") Long idAcceso,
            Pageable pageable);

    Optional<Usuario> findByIdUsuarioAndAccesoNot(Integer idUsuario, Acceso acceso);

    Optional<Usuario> findByCodigoAndAccesoNot(String codigo, Acceso acceso);

    Optional<Usuario> findByCodigo(String codigo);

    Optional<Usuario> findByDocumentoIdentidadAndAccesoNot(String documentoIdentidad, Acceso acceso);

    Optional<Usuario> findByDocumentoIdentidad(String documentoIdentidad);

    long countByCodigoStartingWith(String prefijo);

    Optional<Usuario> findByResetToken(String resetToken);

    Optional<Usuario> findByGmailAndAccesoNot(String gmail, Acceso acceso);

    boolean existsByGmailAndAccesoNot(String gmail, Acceso acceso);
}
