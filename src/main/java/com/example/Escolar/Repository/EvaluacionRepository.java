package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluacionRepository extends JpaRepository<Evaluacion, Integer> {
    Optional<Evaluacion> findByIdEvaluacionAndAccesoNot(Integer idEvaluacion, Acceso acceso);

    List<Evaluacion> findByAsignacionIdAsignacionAndPeriodoAndAccesoNotOrderByFechaAscIdEvaluacionAsc(
            Integer idAsignacion, byte periodo, Acceso acceso);

    List<Evaluacion> findByAsignacionGradoSeccionIdGradoSeccionAndPeriodoAndAccesoNot(
            Integer idGradoSeccion, byte periodo, Acceso acceso);
}
