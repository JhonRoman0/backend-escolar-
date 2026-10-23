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

    long countByDocenteAndAcceso(Docente docente, Acceso acceso);

    /**
     * Una seccion con al menos un curso asignado no se puede modificar ni eliminar:
     * el horario, las notas y la asistencia cuelgan de esa fila.
     */
    boolean existsByGradoSeccionIdGradoSeccionAndAccesoNot(Integer idGradoSeccion, Acceso acceso);
    boolean existsByCursoIdCursoAndAccesoNot(Integer idCurso, Acceso acceso);

    /** Un curso de una seccion tiene un solo docente. */
    List<Asignacion> findByCursoIdCursoAndGradoSeccionIdGradoSeccionAndAccesoNot(
            Integer idCurso, Integer idGradoSeccion, Acceso acceso);
    long countByAnioEscolarIdAnioAndAccesoNot(Integer idAnio, Acceso acceso);

    long countByAnioEscolarIdAnioAndGradoSeccionGradoNivelIdNivelAndAccesoNot(Integer idAnio, Integer idNivel, Acceso acceso);
}
