package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.NotaEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface NotaEvaluacionRepository extends JpaRepository<NotaEvaluacion, Integer> {

    /** Alumnos con notas del curso y año puestas en otra sección. */
    @Query("""
            select distinct n.matricula.idMatricula from NotaEvaluacion n
            where n.matricula.idMatricula in :ids and n.acceso <> :eliminado
              and n.evaluacion.asignacion.curso.idCurso = :curso
              and n.evaluacion.asignacion.anioEscolar.idAnio = :anio
              and n.evaluacion.asignacion.gradoSeccion.idGradoSeccion <> :seccion""")
    List<Integer> alumnosConHistorial(@Param("ids") Collection<Integer> ids, @Param("curso") Integer curso,
                                      @Param("anio") Integer anio, @Param("seccion") Integer seccion,
                                      @Param("eliminado") Acceso eliminado);

    List<NotaEvaluacion> findByMatriculaIdMatriculaAndAccesoNot(Integer idMatricula, Acceso acceso);

    /** Notas de esos alumnos en el periodo, en cualquier sección. */
    List<NotaEvaluacion> findByMatriculaIdMatriculaInAndEvaluacionPeriodoAndAccesoNot(
            Collection<Integer> idsMatricula, byte periodo, Acceso acceso);

    List<NotaEvaluacion> findByEvaluacionIdEvaluacionAndAccesoNot(Integer idEvaluacion, Acceso acceso);

    /** También las borradas: volver a poner la nota reutiliza su fila (una por alumno y evaluación). */
    List<NotaEvaluacion> findByEvaluacionIdEvaluacion(Integer idEvaluacion);

    List<NotaEvaluacion> findByEvaluacionIdEvaluacionInAndAccesoNot(Collection<Integer> idsEvaluacion, Acceso acceso);

    long countByEvaluacionIdEvaluacionAndAccesoNot(Integer idEvaluacion, Acceso acceso);
}
