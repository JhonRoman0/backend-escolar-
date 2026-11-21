package com.example.Escolar.Repository;

import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AlumnoApoderado;
import com.example.Escolar.Model.Matricula;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MatriculaRepository extends JpaRepository<Matricula, Integer> {
    interface PrimerAnioAlumno {
        Integer getIdAlumno();
        String getPrimerAnio();
    }

    interface OcupacionSeccion {
        Integer getIdGradoSeccion();

        long getTotal();
    }

    @Query("SELECT m.alumnoApoderado.alumno.idAlumno AS idAlumno, MIN(m.gradoSeccion.anioEscolar.anio) AS primerAnio " +
            "FROM Matricula m WHERE m.alumnoApoderado.alumno.idAlumno IN :ids " +
            "AND m.acceso.idAcceso <> :eliminadoId GROUP BY m.alumnoApoderado.alumno.idAlumno")
    List<PrimerAnioAlumno> primerosAniosPorAlumno(@Param("ids") List<Integer> ids, @Param("eliminadoId") Long eliminadoId);

    List<Matricula> findByAccesoNot(Acceso acceso);

    List<Matricula> findByGradoSeccionAnioEscolarIdAnioAndAccesoNot(Integer idAnio, Acceso acceso);

    Page<Matricula> findByAccesoNot(Acceso acceso, Pageable pageable);

    Page<Matricula> findByAcceso(Acceso acceso, Pageable pageable);

    List<Matricula> findByAcceso(Acceso acceso);

    Optional<Matricula> findByIdMatriculaAndAccesoNot(Integer idMatricula, Acceso acceso);




    List<Matricula> findByGradoSeccionIdGradoSeccionAndAccesoNot(Integer idGradoSeccion, Acceso acceso);

    boolean existsByGradoSeccionIdGradoSeccionAndAccesoNot(Integer idGradoSeccion, Acceso acceso);

    long countByGradoSeccionIdGradoSeccionAndAccesoNot(Integer idGradoSeccion, Acceso acceso);

    @Query("select m.gradoSeccion.idGradoSeccion as idGradoSeccion, count(m) as total " +
            "from Matricula m where m.gradoSeccion.idGradoSeccion in :ids and m.acceso <> :eliminado " +
            "group by m.gradoSeccion.idGradoSeccion")
    List<OcupacionSeccion> ocupacionPorSecciones(@Param("ids") Collection<Integer> ids,
                                                 @Param("eliminado") Acceso eliminado);

    List<Matricula> findByAlumnoApoderadoAlumnoIdAlumnoAndAccesoNot(Integer idAlumno, Acceso acceso);

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
            "AND (:idAnio IS NULL OR anio.idAnio = :idAnio) " +
            "AND (:idAcceso IS NULL OR m.acceso.idAcceso = :idAcceso)",
            countQuery = "SELECT COUNT(m) FROM Matricula m " +
                    "JOIN m.gradoSeccion gs JOIN gs.grado g JOIN g.nivel n LEFT JOIN gs.seccion s JOIN gs.turno t JOIN gs.anioEscolar anio " +
                    "WHERE m.acceso.idAcceso <> :eliminadoId AND gs.acceso.idAcceso <> :eliminadoId " +
                    "AND (:idNivel IS NULL OR n.idNivel = :idNivel) " +
                    "AND (:idGrado IS NULL OR g.idGrado = :idGrado) " +
                    "AND (:idSeccion IS NULL OR s.idSeccion = :idSeccion) " +
                    "AND (:idTurno IS NULL OR t.idTurno = :idTurno) " +
                    "AND (:idGradoSeccion IS NULL OR gs.idGradoSeccion = :idGradoSeccion) " +
                    "AND (:idAnio IS NULL OR anio.idAnio = :idAnio) " +
                    "AND (:idAcceso IS NULL OR m.acceso.idAcceso = :idAcceso)")
    Page<Matricula> findFiltradas(
            @Param("eliminadoId") Long eliminadoId,
            @Param("idNivel") Integer idNivel,
            @Param("idGrado") Integer idGrado,
            @Param("idSeccion") Integer idSeccion,
            @Param("idTurno") Integer idTurno,
            @Param("idGradoSeccion") Integer idGradoSeccion,
            @Param("idAnio") Integer idAnio,
            @Param("idAcceso") Long idAcceso,
            Pageable pageable);

    @Query("SELECT m FROM Matricula m " +
            "JOIN m.gradoSeccion gs JOIN gs.grado g JOIN g.nivel n LEFT JOIN gs.seccion s JOIN gs.turno t JOIN gs.anioEscolar anio " +
            "WHERE m.acceso.idAcceso <> :eliminadoId AND gs.acceso.idAcceso <> :eliminadoId " +
            "AND (:idNivel IS NULL OR n.idNivel = :idNivel) " +
            "AND (:idGrado IS NULL OR g.idGrado = :idGrado) " +
            "AND (:idSeccion IS NULL OR s.idSeccion = :idSeccion) " +
            "AND (:idTurno IS NULL OR t.idTurno = :idTurno) " +
            "AND (:idGradoSeccion IS NULL OR gs.idGradoSeccion = :idGradoSeccion) " +
            "AND (:idAnio IS NULL OR anio.idAnio = :idAnio) " +
            "AND (:idAcceso IS NULL OR m.acceso.idAcceso = :idAcceso)")
    List<Matricula> findFiltradasList(
            @Param("eliminadoId") Long eliminadoId,
            @Param("idNivel") Integer idNivel,
            @Param("idGrado") Integer idGrado,
            @Param("idSeccion") Integer idSeccion,
            @Param("idTurno") Integer idTurno,
            @Param("idGradoSeccion") Integer idGradoSeccion,
            @Param("idAnio") Integer idAnio,
            @Param("idAcceso") Long idAcceso);

    /**
     * Matrículas del año con solo lo que usan los listados de asistencia (sin cargar
     * sus relaciones EAGER): id, estado, sección y datos del alumno.
     */
    @Query("select m.idMatricula, m.acceso.idAcceso, m.gradoSeccion.idGradoSeccion, a.idAlumno, a.nombre, "
            + "a.apellidoPat, a.apellidoMat, a.codigo, a.documentoIdentidad, a.urlFoto "
            + "from Matricula m join m.alumnoApoderado aa join aa.alumno a "
            + "where m.gradoSeccion.anioEscolar.idAnio = :idAnio and m.acceso <> :eliminado")
    List<Object[]> ligerasDelAnio(@Param("idAnio") Integer idAnio, @Param("eliminado") Acceso eliminado);

    /** Las secciones que tienen matrículas en el año (unas decenas, se cargan una vez). */
    @Query("select distinct m.gradoSeccion from Matricula m "
            + "where m.gradoSeccion.anioEscolar.idAnio = :idAnio and m.acceso <> :eliminado")
    List<GradoSeccion> seccionesConMatriculas(@Param("idAnio") Integer idAnio, @Param("eliminado") Acceso eliminado);
}
