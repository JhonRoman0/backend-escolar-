package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.RolPermisoAccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolPermisoAccionRepository extends JpaRepository<RolPermisoAccion, Integer> {

    List<RolPermisoAccion> findByRolPermisoIdRolPermiso(Integer idRolPermiso);

    List<RolPermisoAccion> findByRolPermisoIdRolPermisoAndAccesoNot(Integer idRolPermiso, Acceso acceso);

    Optional<RolPermisoAccion> findByRolPermisoIdRolPermisoAndAccionIdAccionAndAccesoNot(Integer idRolPermiso, Integer idAccion, Acceso acceso);
}
