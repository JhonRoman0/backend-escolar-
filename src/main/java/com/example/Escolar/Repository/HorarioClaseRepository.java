package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.HorarioClase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HorarioClaseRepository extends JpaRepository<HorarioClase, Integer> {
    List<HorarioClase> findByAccesoNot(Acceso acceso);

    List<HorarioClase> findByAsignacionIdAsignacionAndAccesoNot(Integer idAsignacion, Acceso acceso);

    List<HorarioClase> findByAulaIdAulaAndAccesoNot(Integer idAula, Acceso acceso);

    List<HorarioClase> findByAsignacionDocenteIdDocenteAndAccesoNot(Integer idDocente, Acceso acceso);

    // Réplica patrón Alumnos: 6 filtros académicos + docente + grado/seccion string, vigente, paginado
    @Query(value = "SELECT h FROM HorarioClase h " +
            "JOIN h.asignacion a JOIN a.gradoSeccion gs JOIN gs.grado g JOIN g.nivel n LEFT JOIN gs.seccion s JOIN gs.turno t JOIN a.anioEscolar anio " +
            "WHERE h.acceso <> :eliminado AND a.acceso <> :eliminado AND gs.acceso <> :eliminado " +
            "AND a.anioEscolar.idAnio = :idAnio " +
            "AND (:idNivel IS NULL OR n.idNivel = :idNivel) " +
            "AND (:idGrado IS NULL OR g.idGrado = :idGrado) " +
            "AND (:idSeccion IS NULL OR s.idSeccion = :idSeccion) " +
            "AND (:idTurno IS NULL OR t.idTurno = :idTurno) " +
            "AND (:idGradoSeccion IS NULL OR gs.idGradoSeccion = :idGradoSeccion) " +
            "AND (:idDocente IS NULL OR a.docente.idDocente = :idDocente) " +
            "AND (:grado IS NULL OR LOWER(g.nombre) = LOWER(:grado)) " +
            "AND (:seccion IS NULL OR LOWER(COALESCE(s.nombre, 'Única')) = LOWER(:seccion))",
            countQuery = "SELECT COUNT(h) FROM HorarioClase h " +
                    "JOIN h.asignacion a JOIN a.gradoSeccion gs JOIN gs.grado g JOIN g.nivel n LEFT JOIN gs.seccion s JOIN gs.turno t JOIN a.anioEscolar anio " +
                    "WHERE h.acceso <> :eliminado AND a.acceso <> :eliminado AND gs.acceso <> :eliminado " +
                    "AND a.anioEscolar.idAnio = :idAnio " +
                    "AND (:idNivel IS NULL OR n.idNivel = :idNivel) " +
                    "AND (:idGrado IS NULL OR g.idGrado = :idGrado) " +
                    "AND (:idSeccion IS NULL OR s.idSeccion = :idSeccion) " +
                    "AND (:idTurno IS NULL OR t.idTurno = :idTurno) " +
                    "AND (:idGradoSeccion IS NULL OR gs.idGradoSeccion = :idGradoSeccion) " +
                    "AND (:idDocente IS NULL OR a.docente.idDocente = :idDocente) " +
                    "AND (:grado IS NULL OR LOWER(g.nombre) = LOWER(:grado)) " +
                    "AND (:seccion IS NULL OR LOWER(COALESCE(s.nombre, 'Única')) = LOWER(:seccion))")
    org.springframework.data.domain.Page<HorarioClase> findFiltrados(
            @Param("eliminado") Acceso eliminado,
            @Param("idAnio") Integer idAnio,
            @Param("idNivel") Integer idNivel,
            @Param("idGrado") Integer idGrado,
            @Param("idSeccion") Integer idSeccion,
            @Param("idTurno") Integer idTurno,
            @Param("idGradoSeccion") Integer idGradoSeccion,
            @Param("idDocente") Integer idDocente,
            @Param("grado") String grado,
            @Param("seccion") String seccion,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT h FROM HorarioClase h " +
            "JOIN h.asignacion a JOIN a.gradoSeccion gs JOIN gs.grado g JOIN g.nivel n LEFT JOIN gs.seccion s JOIN gs.turno t JOIN a.anioEscolar anio " +
            "WHERE h.acceso <> :eliminado AND a.acceso <> :eliminado AND gs.acceso <> :eliminado " +
            "AND a.anioEscolar.idAnio = :idAnio " +
            "AND (:idNivel IS NULL OR n.idNivel = :idNivel) " +
            "AND (:idGrado IS NULL OR g.idGrado = :idGrado) " +
            "AND (:idSeccion IS NULL OR s.idSeccion = :idSeccion) " +
            "AND (:idTurno IS NULL OR t.idTurno = :idTurno) " +
            "AND (:idGradoSeccion IS NULL OR gs.idGradoSeccion = :idGradoSeccion) " +
            "AND (:idDocente IS NULL OR a.docente.idDocente = :idDocente) " +
            "AND (:grado IS NULL OR LOWER(g.nombre) = LOWER(:grado)) " +
            "AND (:seccion IS NULL OR LOWER(COALESCE(s.nombre, 'Única')) = LOWER(:seccion))")
    List<HorarioClase> findFiltradosList(
            @Param("eliminado") Acceso eliminado,
            @Param("idAnio") Integer idAnio,
            @Param("idNivel") Integer idNivel,
            @Param("idGrado") Integer idGrado,
            @Param("idSeccion") Integer idSeccion,
            @Param("idTurno") Integer idTurno,
            @Param("idGradoSeccion") Integer idGradoSeccion,
            @Param("idDocente") Integer idDocente,
            @Param("grado") String grado,
            @Param("seccion") String seccion);

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
    List<HorarioClase> findPlanosPorAnio(@Param("eliminado") Acceso eliminado, @Param("idAnio") Integer idAnio);

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
    List<HorarioClase> findPlanosPorDocenteYAnio(@Param("eliminado") Acceso eliminado,
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
    List<HorarioClase> findPlanosPorGradoSeccionYAnio(@Param("eliminado") Acceso eliminado,
                                                       @Param("idAnio") Integer idAnio,
                                                       @Param("idGradoSeccion") Integer idGradoSeccion);
}
