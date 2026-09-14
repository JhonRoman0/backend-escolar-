package com.example.Escolar.Repository;

import com.example.Escolar.Model.HorarioClase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HorarioClaseRepository extends JpaRepository<HorarioClase, Integer> {
    List<HorarioClase> findByAccesoNot(Byte acceso);

    List<HorarioClase> findByAsignacionIdAsignacionAndAccesoNot(Integer idAsignacion, Byte acceso);

    List<HorarioClase> findByAulaIdAulaAndAccesoNot(Integer idAula, Byte acceso);

    List<HorarioClase> findByAsignacionDocenteIdDocenteAndAccesoNot(Integer idDocente, Byte acceso);

    @Query("SELECT h FROM HorarioClase h " +
            "JOIN FETCH h.asignacion a " +
            "JOIN FETCH a.curso " +
            "JOIN FETCH a.docente d " +
            "JOIN FETCH d.usuario " +
            "JOIN FETCH a.gradoSeccion gs " +
            "JOIN FETCH gs.grado " +
            "JOIN FETCH gs.seccion " +
            "JOIN FETCH gs.turno " +
            "JOIN FETCH h.aula " +
            "WHERE h.acceso <> :eliminado AND a.acceso <> :eliminado " +
            "AND a.anioEscolar.idAnio = :idAnio " +
            "ORDER BY h.diaSemana ASC, h.horaInicio ASC")
    List<HorarioClase> findPlanosPorAnio(@Param("eliminado") Byte eliminado, @Param("idAnio") Integer idAnio);

    @Query("SELECT h FROM HorarioClase h " +
            "JOIN FETCH h.asignacion a " +
            "JOIN FETCH a.curso " +
            "JOIN FETCH a.docente d " +
            "JOIN FETCH d.usuario " +
            "JOIN FETCH a.gradoSeccion gs " +
            "JOIN FETCH gs.grado " +
            "JOIN FETCH gs.seccion " +
            "JOIN FETCH gs.turno " +
            "JOIN FETCH h.aula " +
            "WHERE h.acceso <> :eliminado AND a.acceso <> :eliminado " +
            "AND a.anioEscolar.idAnio = :idAnio AND d.idDocente = :idDocente " +
            "ORDER BY h.diaSemana ASC, h.horaInicio ASC")
    List<HorarioClase> findPlanosPorDocenteYAnio(@Param("eliminado") Byte eliminado,
                                                  @Param("idAnio") Integer idAnio,
                                                  @Param("idDocente") Integer idDocente);

    @Query("SELECT h FROM HorarioClase h " +
            "JOIN FETCH h.asignacion a " +
            "JOIN FETCH a.curso " +
            "JOIN FETCH a.docente d " +
            "JOIN FETCH d.usuario " +
            "JOIN FETCH a.gradoSeccion gs " +
            "JOIN FETCH gs.grado " +
            "JOIN FETCH gs.seccion " +
            "JOIN FETCH gs.turno " +
            "JOIN FETCH h.aula " +
            "WHERE h.acceso <> :eliminado AND a.acceso <> :eliminado " +
            "AND a.anioEscolar.idAnio = :idAnio AND gs.idGradoSeccion = :idGradoSeccion " +
            "ORDER BY h.diaSemana ASC, h.horaInicio ASC")
    List<HorarioClase> findPlanosPorGradoSeccionYAnio(@Param("eliminado") Byte eliminado,
                                                       @Param("idAnio") Integer idAnio,
                                                       @Param("idGradoSeccion") Integer idGradoSeccion);
}
