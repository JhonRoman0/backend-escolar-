package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AlumnoApoderado;
import com.example.Escolar.Model.Matricula;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MatriculaRepository extends JpaRepository<Matricula, Integer> {
    List<Matricula> findByAccesoNot(Acceso acceso);

    Page<Matricula> findByAccesoNot(Acceso acceso, Pageable pageable);

    Optional<Matricula> findByIdMatriculaAndAccesoNot(Integer idMatricula, Acceso acceso);

    Optional<Matricula> findByAlumnoApoderadoAndAccesoNot(AlumnoApoderado alumnoApoderado, Acceso acceso);

    Optional<Matricula> findByAlumnoApoderadoAndAcceso(AlumnoApoderado alumnoApoderado, Acceso acceso);

    boolean existsByAlumnoApoderado(AlumnoApoderado alumnoApoderado);

    List<Matricula> findByGradoSeccionIdGradoSeccionAndAccesoNot(Integer idGradoSeccion, Acceso acceso);

    List<Matricula> findByFechaRegistroBetweenAndAccesoNot(LocalDate inicio, LocalDate fin, Acceso acceso);

    @Query("SELECT m FROM Matricula m " +
            "JOIN FETCH m.alumnoApoderado aa " +
            "JOIN FETCH aa.alumno a " +
            "WHERE a.idAlumno = :idAlumno AND m.acceso = :acceso")
    List<Matricula> findActivasPorAlumnoId(@Param("idAlumno") Integer idAlumno, @Param("acceso") Acceso acceso);

    @Query("SELECT m FROM Matricula m " +
            "JOIN FETCH m.gradoSeccion gs " +
            "JOIN FETCH gs.grado g JOIN FETCH g.nivel " +
            "LEFT JOIN FETCH gs.seccion " +
            "JOIN FETCH gs.turno JOIN FETCH gs.anioEscolar " +
            "JOIN FETCH m.alumnoApoderado aa JOIN FETCH aa.alumno a " +
            "WHERE a.idAlumno IN :ids AND m.acceso = :acceso")
    List<Matricula> findActivasPorAlumnoIds(@Param("ids") List<Integer> ids, @Param("acceso") Acceso acceso);

    @Query(value = "SELECT m FROM Matricula m " +
            "JOIN m.gradoSeccion gs JOIN gs.grado g JOIN g.nivel n LEFT JOIN gs.seccion s JOIN gs.turno t JOIN gs.anioEscolar anio " +
            "WHERE m.acceso.idAcceso <> :eliminadoId AND gs.acceso.idAcceso <> :eliminadoId " +
            "AND (:idNivel IS NULL OR n.idNivel = :idNivel) " +
            "AND (:idGrado IS NULL OR g.idGrado = :idGrado) " +
            "AND (:idSeccion IS NULL OR s.idSeccion = :idSeccion) " +
            "AND (:idTurno IS NULL OR t.idTurno = :idTurno) " +
            "AND (:idGradoSeccion IS NULL OR gs.idGradoSeccion = :idGradoSeccion) " +
            "AND (:idAnio IS NULL OR anio.idAnio = :idAnio)",
            countQuery = "SELECT COUNT(m) FROM Matricula m " +
                    "JOIN m.gradoSeccion gs JOIN gs.grado g JOIN g.nivel n LEFT JOIN gs.seccion s JOIN gs.turno t JOIN gs.anioEscolar anio " +
                    "WHERE m.acceso.idAcceso <> :eliminadoId AND gs.acceso.idAcceso <> :eliminadoId " +
                    "AND (:idNivel IS NULL OR n.idNivel = :idNivel) " +
                    "AND (:idGrado IS NULL OR g.idGrado = :idGrado) " +
                    "AND (:idSeccion IS NULL OR s.idSeccion = :idSeccion) " +
                    "AND (:idTurno IS NULL OR t.idTurno = :idTurno) " +
                    "AND (:idGradoSeccion IS NULL OR gs.idGradoSeccion = :idGradoSeccion) " +
                    "AND (:idAnio IS NULL OR anio.idAnio = :idAnio)")
    Page<Matricula> findFiltradas(
            @Param("eliminadoId") Long eliminadoId,
            @Param("idNivel") Integer idNivel,
            @Param("idGrado") Integer idGrado,
            @Param("idSeccion") Integer idSeccion,
            @Param("idTurno") Integer idTurno,
            @Param("idGradoSeccion") Integer idGradoSeccion,
            @Param("idAnio") Integer idAnio,
            Pageable pageable);

    @Query("SELECT m FROM Matricula m " +
            "JOIN m.gradoSeccion gs JOIN gs.grado g JOIN g.nivel n LEFT JOIN gs.seccion s JOIN gs.turno t JOIN gs.anioEscolar anio " +
            "WHERE m.acceso.idAcceso <> :eliminadoId AND gs.acceso.idAcceso <> :eliminadoId " +
            "AND (:idNivel IS NULL OR n.idNivel = :idNivel) " +
            "AND (:idGrado IS NULL OR g.idGrado = :idGrado) " +
            "AND (:idSeccion IS NULL OR s.idSeccion = :idSeccion) " +
            "AND (:idTurno IS NULL OR t.idTurno = :idTurno) " +
            "AND (:idGradoSeccion IS NULL OR gs.idGradoSeccion = :idGradoSeccion) " +
            "AND (:idAnio IS NULL OR anio.idAnio = :idAnio)")
    List<Matricula> findFiltradasList(
            @Param("eliminadoId") Long eliminadoId,
            @Param("idNivel") Integer idNivel,
            @Param("idGrado") Integer idGrado,
            @Param("idSeccion") Integer idSeccion,
            @Param("idTurno") Integer idTurno,
            @Param("idGradoSeccion") Integer idGradoSeccion,
            @Param("idAnio") Integer idAnio);
}