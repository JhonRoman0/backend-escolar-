package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.NotaEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface NotaEvaluacionRepository extends JpaRepository<NotaEvaluacion, Integer> {
    List<NotaEvaluacion> findByEvaluacionIdEvaluacionAndAccesoNot(Integer idEvaluacion, Acceso acceso);

    /** También las borradas: volver a poner la nota reutiliza su fila (una por alumno y evaluación). */
    List<NotaEvaluacion> findByEvaluacionIdEvaluacion(Integer idEvaluacion);

    List<NotaEvaluacion> findByEvaluacionIdEvaluacionInAndAccesoNot(Collection<Integer> idsEvaluacion, Acceso acceso);

    long countByEvaluacionIdEvaluacionAndAccesoNot(Integer idEvaluacion, Acceso acceso);
}
