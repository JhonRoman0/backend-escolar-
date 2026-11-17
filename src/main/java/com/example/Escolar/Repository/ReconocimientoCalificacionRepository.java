package com.example.Escolar.Repository;

import com.example.Escolar.Model.ReconocimientoCalificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReconocimientoCalificacionRepository extends JpaRepository<ReconocimientoCalificacion, Integer> {

    /** Un reconocimiento visto desde el alumno: nota de origen, nota y evaluación de destino. */
    interface Movimiento {
        Integer getMatricula();
        Integer getOrigen();
        Integer getDestino();
        Integer getEvaluacion();
        Integer getCurso();
        Integer getAnio();
        Integer getId();
        Byte getVigencia();
        String getGrado();
        String getSeccion();
        String getNombre();
    }

    @Query("""
            select r.notaOrigen.matricula.idMatricula as matricula, r.notaOrigen.idNotaEvaluacion as origen,
                   r.notaDestino.idNotaEvaluacion as destino, r.notaDestino.evaluacion.idEvaluacion as evaluacion,
                   r.notaOrigen.evaluacion.asignacion.curso.idCurso as curso,
                   r.notaOrigen.evaluacion.asignacion.anioEscolar.idAnio as anio, r.idReconocimiento as id,
                   r.vigencia as vigencia, gs.grado.nombre as grado, s.nombre as seccion,
                   r.notaOrigen.evaluacion.nombre as nombre
            from ReconocimientoCalificacion r
            join r.notaOrigen.evaluacion.asignacion.gradoSeccion gs
            left join gs.seccion s
            where r.notaOrigen.matricula.idMatricula in :ids""")
    List<Movimiento> movimientosDe(@Param("ids") Collection<Integer> ids);

    @Query("""
            select count(r) > 0 from ReconocimientoCalificacion r
            where r.vigencia = :vigencia
              and (r.notaOrigen.evaluacion.asignacion.idAsignacion = :id
                   or r.notaDestino.evaluacion.asignacion.idAsignacion = :id)""")
    boolean tieneAsignacionActiva(@Param("id") Integer id, @Param("vigencia") Byte vigencia);

    List<ReconocimientoCalificacion> findByNotaOrigenMatriculaIdMatricula(Integer idMatricula);

    boolean existsByNotaOrigenIdNotaEvaluacionAndVigencia(Integer idNotaOrigen, Byte vigencia);

    /** Solo los activos: una nota cuyo reconocimiento se anuló vuelve a ser una nota normal. */
    boolean existsByNotaDestinoIdNotaEvaluacionAndVigencia(Integer idNotaDestino, Byte vigencia);

    boolean existsByNotaOrigenIdNotaEvaluacionAndSeccionDestinoIdGradoSeccionAndVigencia(
            Integer idNotaOrigen, Integer idSeccionDestino, Byte vigencia);
}
