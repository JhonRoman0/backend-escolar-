package com.example.Escolar.Repository;

import com.example.Escolar.Model.Competencia;
import com.example.Escolar.Model.Acceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompetenciaRepository extends JpaRepository<Competencia, Integer> {
    Optional<Competencia> findByIdCompetenciaAndAccesoNot(Integer id, Acceso acceso);
    List<Competencia> findByCursoIdCursoAndAccesoNot(Integer idCurso, Acceso acceso);
    List<Competencia> findByCursoIdCursoInAndAccesoNot(Collection<Integer> idsCurso, Acceso acceso);
    List<Competencia> findByAccesoNot(Acceso acceso);
    List<Competencia> findByCursoIdCurso(Integer idCurso);
}
