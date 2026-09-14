package com.example.Escolar.Repository;

import com.example.Escolar.Model.PermisoAccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermisoAccionRepository extends JpaRepository<PermisoAccion, Integer> {

    List<PermisoAccion> findByPermisoIdPermiso(Integer idPermiso);

    List<PermisoAccion> findByPermisoIdPermisoAndAccesoNot(Integer idPermiso, Byte acceso);

    Optional<PermisoAccion> findByPermisoIdPermisoAndAccionIdAccion(Integer idPermiso, Integer idAccion);
}
