package com.example.Escolar.Repository;

import com.example.Escolar.Model.AsistenciaAlumno;
import com.example.Escolar.Model.Acceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AsistenciaAlumnoRepository extends JpaRepository<AsistenciaAlumno, Integer> {

    List<AsistenciaAlumno> findByAccesoNot(Acceso acceso);

    Optional<AsistenciaAlumno> findByIdAsistenciaAndAccesoNot(Integer idAsistencia, Acceso acceso);

    Optional<AsistenciaAlumno> findByMatriculaIdMatriculaAndFechaAndAccesoNot(Integer idMatricula, LocalDate fecha, Acceso acceso);

    boolean existsByMatriculaIdMatriculaAndFechaAndAccesoNot(Integer idMatricula, LocalDate fecha, Acceso acceso);

    List<AsistenciaAlumno> findByFechaBetweenAndAccesoNot(LocalDate inicio, LocalDate fin, Acceso acceso);

    List<AsistenciaAlumno> findByFechaBetweenAndAccesoNotOrderByFechaAscHoraEntradaAsc(LocalDate inicio, LocalDate fin, Acceso acceso);

    List<AsistenciaAlumno> findByMatriculaIdMatriculaAndFechaBetweenAndAccesoNot(Integer idMatricula, LocalDate inicio, LocalDate fin, Acceso acceso);

    List<AsistenciaAlumno> findByUsuarioRegistroIdUsuarioAndFechaBetweenAndAccesoNot(Integer idUsuario, LocalDate inicio, LocalDate fin, Acceso acceso);
}