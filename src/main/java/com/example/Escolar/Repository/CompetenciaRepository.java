package com.example.Escolar.Repository;

import com.example.Escolar.Model.Competencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompetenciaRepository extends JpaRepository<Competencia, Integer> {
    Optional<Competencia> findByIdCompetenciaAndAccesoNot(Integer id, byte acceso);
    List<Competencia> findByCursoIdCursoAndAccesoNot(Integer idCurso, byte acceso);
    Optional<Competencia> findByCursoIdCursoAndNombreAndAccesoNot(Integer idCurso, String nombre, byte acceso);
    Optional<Competencia> findByNombreAndAccesoNot(String nombre, byte acceso);
}
