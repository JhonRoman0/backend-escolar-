package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer>, UsuarioBusquedaRepository {

    List<Usuario> findByAccesoNot(Acceso acceso);

    Page<Usuario> findByAccesoNot(Acceso acceso, Pageable pageable);

    Optional<Usuario> findByIdUsuarioAndAccesoNot(Integer idUsuario, Acceso acceso);

    Optional<Usuario> findByCodigoAndAccesoNot(String codigo, Acceso acceso);

    Optional<Usuario> findByCodigo(String codigo);

    Optional<Usuario> findByDocumentoIdentidadAndAccesoNot(String documentoIdentidad, Acceso acceso);

    Optional<Usuario> findByDocumentoIdentidad(String documentoIdentidad);

    boolean existsByDocumentoIdentidad(String documentoIdentidad);

    long countByCodigoStartingWith(String prefijo);

    Optional<Usuario> findByResetToken(String resetToken);

    Optional<Usuario> findByDosFactorDesafio(String dosFactorDesafio);

    /** ¿El dispositivo de la sesión sigue registrado para el usuario? (búsqueda por llave primaria) */
    @Query("select count(d) > 0 from DispositivoUsuario d where d.idDispositivo = :dispositivo "
            + "and d.usuario.idUsuario = :id")
    boolean tieneDispositivo(@Param("id") Integer id, @Param("dispositivo") Integer dispositivo);

    /** Códigos de los administradores activos sin correo (no podrían recibir el código). */
    @Query("select u.codigo from UsuarioRol ur join ur.usuario u join ur.rol r "
            + "where upper(r.nombre) = 'ADMIN' and r.acceso.idAcceso <> :eliminado "
            + "and u.acceso.idAcceso = :activo and (u.gmail is null or trim(u.gmail) = '')")
    List<String> administradoresSinCorreo(@Param("activo") Long activo, @Param("eliminado") Long eliminado);

    Optional<Usuario> findByGmailAndAccesoNot(String gmail, Acceso acceso);

    boolean existsByGmailAndAccesoNot(String gmail, Acceso acceso);

    @Query("select u.idUsuario as idUsuario, u.fechaInicioVigencia as fechaInicioVigencia, "
            + "u.fechaFinVigencia as fechaFinVigencia, u.versionToken as versionToken, "
            + "u.acceso.idAcceso as accesoId from Usuario u "
            + "where u.idUsuario = :id and u.acceso.idAcceso <> :eliminado")
    Optional<UsuarioVigenciaSesion> findVigenciaSesion(@Param("id") Integer id, @Param("eliminado") Long eliminado);
}
