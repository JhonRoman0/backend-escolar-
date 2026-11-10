package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.NotaEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface NotaEvaluacionRepository extends JpaRepository<NotaEvaluacion, Integer> {
    @org.springframework.data.jpa.repository.Query("select distinct n.matricula.idMatricula from NotaEvaluacion n where n.matricula.idMatricula in :ids and n.acceso <> :eliminado and n.evaluacion.asignacion.curso.idCurso = :curso and n.evaluacion.asignacion.anioEscolar.idAnio = :anio and n.evaluacion.asignacion.gradoSeccion.idGradoSeccion <> :seccion")
    List<Integer> alumnosConHistorial(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Integer> ids,
        @org.springframework.data.repository.query.Param("curso") Integer curso, @org.springframework.data.repository.query.Param("anio") Integer anio,
        @org.springframework.data.repository.query.Param("seccion") Integer seccion, @org.springframework.data.repository.query.Param("eliminado") Acceso eliminado);
    List<NotaEvaluacion> findByMatriculaIdMatriculaAndAccesoNot(Integer idMatricula, Acceso acceso);
    List<NotaEvaluacion> findByEvaluacionIdEvaluacionAndAccesoNot(Integer idEvaluacion, Acceso acceso);

    /** También las borradas: volver a poner la nota reutiliza su fila (una por alumno y evaluación). */
    List<NotaEvaluacion> findByEvaluacionIdEvaluacion(Integer idEvaluacion);

    List<NotaEvaluacion> findByEvaluacionIdEvaluacionInAndAccesoNot(Collection<Integer> idsEvaluacion, Acceso acceso);

    long countByEvaluacionIdEvaluacionAndAccesoNot(Integer idEvaluacion, Acceso acceso);
}
