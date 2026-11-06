package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Parentesco;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParentescoRepository extends JpaRepository<Parentesco, Integer> {
    List<Parentesco> findByAccesoNotOrderByIdParentesco(Acceso acceso);

    Optional<Parentesco> findByIdParentescoAndAccesoNot(Integer id, Acceso acceso);

    Optional<Parentesco> findByNombre(String nombre);
}
