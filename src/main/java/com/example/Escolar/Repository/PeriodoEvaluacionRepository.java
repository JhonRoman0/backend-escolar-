package com.example.Escolar.Repository;

import com.example.Escolar.Model.PeriodoEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PeriodoEvaluacionRepository extends JpaRepository<PeriodoEvaluacion, Integer> {
    List<PeriodoEvaluacion> findByAnioEscolarIdAnioAndNivelIdNivelOrderByNumero(Integer idAnio, Integer idNivel);

    List<PeriodoEvaluacion> findByAnioEscolarIdAnio(Integer idAnio);

    long countByAnioEscolarIdAnioAndNivelIdNivel(Integer idAnio, Integer idNivel);

    void deleteByAnioEscolarIdAnio(Integer idAnio);
}
