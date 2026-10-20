package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Recreo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecreoRepository extends JpaRepository<Recreo, Integer> {
    List<Recreo> findByAccesoNot(Acceso acceso);

    Optional<Recreo> findByIdRecreoAndAccesoNot(Integer idRecreo, Acceso acceso);

    /** Los recreos de un grado en un turno y año: los de todas sus secciones. */
    List<Recreo> findByAnioEscolarIdAnioAndTurnoIdTurnoAndGradoIdGradoAndAccesoNot(
            Integer idAnio, Integer idTurno, Integer idGrado, Acceso acceso);
}
