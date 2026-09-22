package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermisoRepository extends JpaRepository<Permiso, Integer> {

    List<Permiso> findByAccesoNot(Acceso acceso);

    List<Permiso> findByModuloIdModulo(Integer idModulo);

    Optional<Permiso> findByIdPermisoAndAccesoNot(Integer idPermiso, Acceso acceso);

    Optional<Permiso> findByCodigoAndAccesoNot(String codigo, Acceso acceso);
}
