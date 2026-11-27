package com.example.Escolar.Repository;

import com.example.Escolar.Model.UsuarioRol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, Integer> {
    List<UsuarioRol> findByRolIdRol(Integer idRol);

    List<UsuarioRol> findByUsuarioIdUsuario(Integer idUsuario);

    void deleteByUsuarioIdUsuario(Integer idUsuario);
    @Query("""
            select distinct ur.usuario.idUsuario from UsuarioRol ur
            where ur.usuario.acceso.idAcceso <> :eliminado
              and ur.rol.idRol in (
                  select rpa.rolPermiso.rol.idRol from RolPermisoAccion rpa
                  where rpa.accion.idAccion = :idAccion and rpa.acceso.idAcceso = :activo
                    and rpa.rolPermiso.acceso.idAcceso = :activo
                    and rpa.rolPermiso.rol.acceso.idAcceso = :activo
              )
            """)
    List<Integer> idsUsuariosConAccion(@Param("idAccion") Integer idAccion,
                                      @Param("activo") Long activo, @Param("eliminado") Long eliminado);
}
