package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Usuario u set u.dosFactorDesafio = null, u.dosFactorDesafioExpiracion = null "
            + "where u.idUsuario = :idUsuario and u.dosFactorDesafio = :hash "
            + "and u.dosFactorDesafioExpiracion > :ahora and u.acceso.idAcceso = :activo "
            + "and coalesce(u.versionToken, 0) = :versionToken")
    int consumirDesafio(@Param("idUsuario") Integer idUsuario, @Param("hash") String hash,
                        @Param("ahora") LocalDateTime ahora, @Param("activo") Long activo,
                        @Param("versionToken") int versionToken);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Usuario u set u.dosFactorDesafio = :hash, u.dosFactorDesafioExpiracion = :expiracion "
            + "where u.idUsuario = :idUsuario")
    int guardarDesafio(@Param("idUsuario") Integer idUsuario, @Param("hash") String hash,
                       @Param("expiracion") LocalDateTime expiracion);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Usuario u set u.dosFactorCodigo = :codigoHash, "
            + "u.dosFactorCodigoExpiracion = :codigoExpiracion, u.dosFactorEnviado = :enviado, "
            + "u.dosFactorEnvios = :envios, u.dosFactorVentana = :ventana "
            + "where u.idUsuario = :idUsuario and u.dosFactorDesafio = :desafioHash")
    int guardarCodigo(@Param("idUsuario") Integer idUsuario, @Param("desafioHash") String desafioHash,
                      @Param("codigoHash") String codigoHash, @Param("codigoExpiracion") LocalDateTime codigoExpiracion,
                      @Param("enviado") LocalDateTime enviado, @Param("envios") Integer envios,
                      @Param("ventana") LocalDateTime ventana);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Usuario u set u.dosFactorDesafio = null, u.dosFactorDesafioExpiracion = null, "
            + "u.dosFactorCodigo = null, u.dosFactorCodigoExpiracion = null "
            + "where u.idUsuario = :idUsuario and u.dosFactorDesafio = :desafioHash")
    int limpiarDesafioPorHash(@Param("idUsuario") Integer idUsuario, @Param("desafioHash") String desafioHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Usuario u set u.intentosFallidos = :intentos, u.fechaBloqueo = :fechaBloqueo "
            + "where u.idUsuario = :idUsuario and u.dosFactorDesafio = :desafioHash")
    int registrarIntentos(@Param("idUsuario") Integer idUsuario, @Param("desafioHash") String desafioHash,
                          @Param("intentos") Integer intentos, @Param("fechaBloqueo") LocalDateTime fechaBloqueo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Usuario u set u.dosFactorDesafio = null, u.dosFactorDesafioExpiracion = null, "
            + "u.dosFactorCodigo = null, u.dosFactorCodigoExpiracion = null, "
            + "u.intentosFallidos = 0, u.fechaBloqueo = null, "
            + "u.dosFactorEnvios = 0, u.dosFactorVentana = null "
            + "where u.idUsuario = :idUsuario and u.dosFactorDesafio = :hash "
            + "and u.dosFactorDesafioExpiracion > :ahora and u.acceso.idAcceso = :activo "
            + "and coalesce(u.versionToken, 0) = :versionToken")
    int consumirDesafioDosFactor(@Param("idUsuario") Integer idUsuario, @Param("hash") String hash,
                                 @Param("ahora") LocalDateTime ahora, @Param("activo") Long activo,
                                 @Param("versionToken") int versionToken);

    /** ¿El dispositivo de la sesión sigue registrado para el usuario? (búsqueda por llave primaria) */
    @Query("select count(d) > 0 from DispositivoUsuario d where d.idDispositivo = :dispositivo "
            + "and d.usuario.idUsuario = :id")
    boolean tieneDispositivo(@Param("id") Integer id, @Param("dispositivo") Integer dispositivo);

    /** Códigos de los administradores activos sin correo (no podrían recibir el código). */
    @Query("select u.codigo from UsuarioRol ur join ur.usuario u join ur.rol r "
            + "where upper(r.nombre) = 'ADMIN' and r.acceso.idAcceso <> :eliminado "
            + "and u.acceso.idAcceso = :activo and (u.gmail is null or trim(u.gmail) = '')")
    List<String> administradoresSinCorreo(@Param("activo") Long activo, @Param("eliminado") Long eliminado);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Usuario u set u.versionToken = coalesce(u.versionToken, 0) + 1 "
            + "where u.idUsuario in :ids and u.acceso.idAcceso <> :eliminado")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    int incrementarVersionToken(@Param("ids") Collection<Integer> ids, @Param("eliminado") Long eliminado);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Usuario u set u.versionToken = coalesce(u.versionToken, 0) + 1 "
            + "where u.idUsuario = :idUsuario and u.acceso.idAcceso <> :eliminado "
            + "and coalesce(u.versionToken, 0) = :versionToken")
    int incrementarVersionTokenSiCoincide(@Param("idUsuario") Integer idUsuario,
                                          @Param("versionToken") int versionToken,
                                          @Param("eliminado") Long eliminado);

    Optional<Usuario> findByGmailAndAccesoNot(String gmail, Acceso acceso);

    boolean existsByGmailAndAccesoNot(String gmail, Acceso acceso);

    @Query("select u.idUsuario as idUsuario, u.fechaInicioVigencia as fechaInicioVigencia, "
            + "u.fechaFinVigencia as fechaFinVigencia, u.versionToken as versionToken, "
            + "u.acceso.idAcceso as accesoId from Usuario u "
            + "where u.idUsuario = :id and u.acceso.idAcceso <> :eliminado")
    Optional<UsuarioVigenciaSesion> findVigenciaSesion(@Param("id") Integer id, @Param("eliminado") Long eliminado);
}
