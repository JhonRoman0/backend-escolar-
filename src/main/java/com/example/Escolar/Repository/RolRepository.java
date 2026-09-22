package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Integer> {

    List<Rol> findByAccesoNot(Acceso acceso);

    Optional<Rol> findByIdRolAndAccesoNot(Integer idRol, Acceso acceso);

    Optional<Rol> findByNombre(String nombre);
}
