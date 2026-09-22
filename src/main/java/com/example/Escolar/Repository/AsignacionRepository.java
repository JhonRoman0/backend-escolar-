package com.example.Escolar.Repository;

import com.example.Escolar.Model.Asignacion;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Docente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AsignacionRepository extends JpaRepository<Asignacion, Integer> {
    List<Asignacion> findByAccesoNot(Acceso acceso);

    Optional<Asignacion> findByIdAsignacionAndAccesoNot(Integer idAsignacion, Acceso acceso);

    List<Asignacion> findByDocenteIdDocenteAndAccesoNot(Integer idDocente, Acceso acceso);

    List<Asignacion> findByDocenteIdDocenteAndAcceso(Integer idDocente, Acceso acceso);

    Optional<Asignacion> findByCursoIdCursoAndDocenteIdDocenteAndGradoSeccionIdGradoSeccionAndAnioEscolarIdAnioAndAccesoNot(
            Integer idCurso, Integer idDocente, Integer idGradoSeccion, Integer idAnio, Acceso acceso);

    long countByDocenteAndAcceso(Docente docente, Acceso acceso);
}
