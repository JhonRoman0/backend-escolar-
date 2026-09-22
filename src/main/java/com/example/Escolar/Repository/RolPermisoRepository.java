package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.RolPermiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolPermisoRepository extends JpaRepository<RolPermiso, Integer> {

    List<RolPermiso> findByAccesoNot(Acceso acceso);

    List<RolPermiso> findByRolIdRol(Integer idRol);

    List<RolPermiso> findByRolIdRolAndAccesoNot(Integer idRol, Acceso acceso);

    List<RolPermiso> findByPermisoIdPermiso(Integer idPermiso);

    Optional<RolPermiso> findByIdRolPermisoAndAccesoNot(Integer idRolPermiso, Acceso acceso);

    Optional<RolPermiso> findByRolIdRolAndPermisoIdPermiso(Integer idRol, Integer idPermiso);
}
