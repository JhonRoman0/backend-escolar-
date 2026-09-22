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
}