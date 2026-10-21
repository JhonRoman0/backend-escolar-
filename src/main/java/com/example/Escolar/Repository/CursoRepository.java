package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CursoRepository extends JpaRepository<Curso, Integer> {
    List<Curso> findByAccesoNot(Acceso acceso);

    List<Curso> findByAnioEscolarIdAnioAndAccesoNot(Integer idAnio, Acceso acceso);

    List<Curso> findByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(Integer idAnio, Integer idNivel, Acceso acceso);

    boolean existsByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(Integer idAnio, Integer idNivel, Acceso acceso);

    long countByAnioEscolarIdAnioAndAccesoNot(Integer idAnio, Acceso acceso);

    List<Curso> findByAnioEscolarIdAnioAndAcceso(Integer idAnio, Acceso acceso);

    List<Curso> findByCursoPadreIdCursoAndAccesoNot(Integer idCursoPadre, Acceso acceso);

    Optional<Curso> findByIdCursoAndAccesoNot(Integer idCurso, Acceso acceso);
}
