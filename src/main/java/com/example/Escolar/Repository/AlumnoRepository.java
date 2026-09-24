package com.example.Escolar.Repository;

import com.example.Escolar.Model.Alumno;
import com.example.Escolar.Model.Acceso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AlumnoRepository extends JpaRepository<Alumno, Integer> {

    List<Alumno> findByAccesoNot(Acceso acceso);

    Page<Alumno> findByAccesoNot(Acceso acceso, Pageable pageable);

    // Filtrado vigente vía Matricula ACTIVA -> GradoSeccion -> Grado/Nivel/Turno/Anio. Usado por GET /alumnos con 6 filtros.
    @org.springframework.data.jpa.repository.Query(
            value = "SELECT DISTINCT a FROM Alumno a " +
                    "JOIN AlumnoApoderado aa ON aa.alumno = a AND aa.apoPrincipal = 1 " +
                    "JOIN Matricula m ON m.alumnoApoderado = aa AND m.acceso.idAcceso = :accesoActivoId " +
                    "JOIN m.gradoSeccion gs " +
                    "JOIN gs.grado g JOIN g.nivel n LEFT JOIN gs.seccion s JOIN gs.turno t JOIN gs.anioEscolar anio " +
                    "WHERE a.acceso.idAcceso <> :eliminadoId " +
                    "AND gs.acceso.idAcceso <> :eliminadoId " +
                    "AND (:idNivel IS NULL OR n.idNivel = :idNivel) " +
                    "AND (:idGrado IS NULL OR g.idGrado = :idGrado) " +
                    "AND (:idSeccion IS NULL OR s.idSeccion = :idSeccion) " +
                    "AND (:idTurno IS NULL OR t.idTurno = :idTurno) " +
                    "AND (:idGradoSeccion IS NULL OR gs.idGradoSeccion = :idGradoSeccion) " +
                    "AND (:idAnio IS NULL OR anio.idAnio = :idAnio)",
            countQuery = "SELECT COUNT(DISTINCT a) FROM Alumno a " +
                    "JOIN AlumnoApoderado aa ON aa.alumno = a AND aa.apoPrincipal = 1 " +
                    "JOIN Matricula m ON m.alumnoApoderado = aa AND m.acceso.idAcceso = :accesoActivoId " +
                    "JOIN m.gradoSeccion gs " +
                    "JOIN gs.grado g JOIN g.nivel n LEFT JOIN gs.seccion s JOIN gs.turno t JOIN gs.anioEscolar anio " +
                    "WHERE a.acceso.idAcceso <> :eliminadoId " +
                    "AND gs.acceso.idAcceso <> :eliminadoId " +
                    "AND (:idNivel IS NULL OR n.idNivel = :idNivel) " +
                    "AND (:idGrado IS NULL OR g.idGrado = :idGrado) " +
                    "AND (:idSeccion IS NULL OR s.idSeccion = :idSeccion) " +
                    "AND (:idTurno IS NULL OR t.idTurno = :idTurno) " +
                    "AND (:idGradoSeccion IS NULL OR gs.idGradoSeccion = :idGradoSeccion) " +
                    "AND (:idAnio IS NULL OR anio.idAnio = :idAnio)"
    )
    Page<Alumno> findFiltrados(
            @org.springframework.data.repository.query.Param("accesoActivoId") Long accesoActivoId,
            @org.springframework.data.repository.query.Param("eliminadoId") Long eliminadoId,
            @org.springframework.data.repository.query.Param("idNivel") Integer idNivel,
            @org.springframework.data.repository.query.Param("idGrado") Integer idGrado,
            @org.springframework.data.repository.query.Param("idSeccion") Integer idSeccion,
            @org.springframework.data.repository.query.Param("idTurno") Integer idTurno,
            @org.springframework.data.repository.query.Param("idGradoSeccion") Integer idGradoSeccion,
            @org.springframework.data.repository.query.Param("idAnio") Integer idAnio,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT DISTINCT a FROM Alumno a " +
                    "JOIN AlumnoApoderado aa ON aa.alumno = a AND aa.apoPrincipal = 1 " +
                    "JOIN Matricula m ON m.alumnoApoderado = aa AND m.acceso.idAcceso = :accesoActivoId " +
                    "JOIN m.gradoSeccion gs " +
                    "JOIN gs.grado g JOIN g.nivel n LEFT JOIN gs.seccion s JOIN gs.turno t JOIN gs.anioEscolar anio " +
                    "WHERE a.acceso.idAcceso <> :eliminadoId " +
                    "AND gs.acceso.idAcceso <> :eliminadoId " +
                    "AND (:idNivel IS NULL OR n.idNivel = :idNivel) " +
                    "AND (:idGrado IS NULL OR g.idGrado = :idGrado) " +
                    "AND (:idSeccion IS NULL OR s.idSeccion = :idSeccion) " +
                    "AND (:idTurno IS NULL OR t.idTurno = :idTurno) " +
                    "AND (:idGradoSeccion IS NULL OR gs.idGradoSeccion = :idGradoSeccion) " +
                    "AND (:idAnio IS NULL OR anio.idAnio = :idAnio)"
    )
    List<Alumno> findFiltradosList(
            @org.springframework.data.repository.query.Param("accesoActivoId") Long accesoActivoId,
            @org.springframework.data.repository.query.Param("eliminadoId") Long eliminadoId,
            @org.springframework.data.repository.query.Param("idNivel") Integer idNivel,
            @org.springframework.data.repository.query.Param("idGrado") Integer idGrado,
            @org.springframework.data.repository.query.Param("idSeccion") Integer idSeccion,
            @org.springframework.data.repository.query.Param("idTurno") Integer idTurno,
            @org.springframework.data.repository.query.Param("idGradoSeccion") Integer idGradoSeccion,
            @org.springframework.data.repository.query.Param("idAnio") Integer idAnio);

    Optional<Alumno> findByIdAlumnoAndAccesoNot(Integer idAlumno, Acceso acceso);

    Optional<Alumno> findByCodigo(String codigo);

    Optional<Alumno> findByCodigoAndAccesoNot(String codigo, Acceso acceso);

    Optional<Alumno> findByCodigoHashAndAccesoNot(String codigoHash, Acceso acceso);

    Optional<Alumno> findByDocumentoIdentidadAndAccesoNot(String documentoIdentidad, Acceso acceso);

    Optional<Alumno> findByDocumentoIdentidad(String documentoIdentidad);

    long countByCodigoStartingWith(String prefijo);

    List<Alumno> findByFechaIngresoBetweenAndAccesoNot(LocalDate inicio, LocalDate fin, Acceso acceso);
}