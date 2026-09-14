package com.example.Escolar.Repository;

import com.example.Escolar.Model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Integer> {

    List<Rol> findByAccesoNot(Byte acceso);

    Optional<Rol> findByIdRolAndAccesoNot(Integer idRol, Byte acceso);

    Optional<Rol> findByNombre(String nombre);
}
