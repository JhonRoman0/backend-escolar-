package com.example.Escolar.Repository;

import com.example.Escolar.Model.RolPermiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolPermisoRepository extends JpaRepository<RolPermiso, Integer> {

    List<RolPermiso> findByAccesoNot(Byte acceso);

    List<RolPermiso> findByRolIdRol(Integer idRol);

    List<RolPermiso> findByRolIdRolAndAccesoNot(Integer idRol, Byte acceso);

    List<RolPermiso> findByPermisoIdPermiso(Integer idPermiso);

    Optional<RolPermiso> findByIdRolPermisoAndAccesoNot(Integer idRolPermiso, Byte acceso);

    Optional<RolPermiso> findByRolIdRolAndPermisoIdPermiso(Integer idRol, Integer idPermiso);
}
