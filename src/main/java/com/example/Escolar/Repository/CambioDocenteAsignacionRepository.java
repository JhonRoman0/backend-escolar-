package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.CambioDocenteAsignacion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CambioDocenteAsignacionRepository extends JpaRepository<CambioDocenteAsignacion, Integer> {

    @EntityGraph(attributePaths = {"asignacion.curso", "asignacion.gradoSeccion.grado",
            "asignacion.gradoSeccion.seccion", "asignacion.anioEscolar"})
    List<CambioDocenteAsignacion> findByCambioIdCambioAndAccesoNot(Integer idCambio, Acceso acceso);

    @EntityGraph(attributePaths = {"asignacion.curso", "asignacion.gradoSeccion.grado",
            "asignacion.gradoSeccion.seccion", "asignacion.anioEscolar"})
    List<CambioDocenteAsignacion> findByCambioIdCambioInAndAccesoNot(
            Collection<Integer> idsCambio, Acceso acceso);

    List<CambioDocenteAsignacion> findByCambioIdCambioAndDevueltaFalseAndAccesoNot(Integer idCambio, Acceso acceso);

    List<CambioDocenteAsignacion> findByAsignacionIdAsignacionInAndDevueltaFalseAndAccesoNot(
            Collection<Integer> idsAsignacion, Acceso acceso);

    List<CambioDocenteAsignacion> findByDocenteHaciaIdDocenteAndDevueltaFalseAndAccesoNot(
            Integer idDocenteHacia, Acceso acceso);

    boolean existsByCambioIdCambioAndAsignacionIdAsignacion(Integer idCambio, Integer idAsignacion);
}
