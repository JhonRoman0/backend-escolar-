package com.example.Escolar.Repository;

import com.example.Escolar.Model.Asignacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AsignacionRepository extends JpaRepository<Asignacion, Integer> {
    List<Asignacion> findByAccesoNot(Byte acceso);

    Optional<Asignacion> findByIdAsignacionAndAccesoNot(Integer idAsignacion, Byte acceso);

    List<Asignacion> findByDocenteIdDocenteAndAccesoNot(Integer idDocente, Byte acceso);

    Optional<Asignacion> findByCursoIdCursoAndDocenteIdDocenteAndGradoSeccionIdGradoSeccionAndAnioEscolarIdAnioAndAccesoNot(
            Integer idCurso, Integer idDocente, Integer idGradoSeccion, Integer idAnio, Byte acceso);
}
