package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CursoRepository extends JpaRepository<Curso, Integer> {
    List<Curso> findByAccesoNot(Acceso acceso);

    Optional<Curso> findByIdCursoAndAccesoNot(Integer idCurso, Acceso acceso);

    Optional<Curso> findByNombreAndAccesoNot(String nombre, Acceso acceso);

    Optional<Curso> findByCodigoAndAccesoNot(String codigo, Acceso acceso);

    boolean existsByCodigoAndAccesoNot(String codigo, Acceso acceso);

    Optional<Curso> findByCodigo(String codigo);

    Optional<Curso> findByNombreIgnoreCase(String nombre);
}