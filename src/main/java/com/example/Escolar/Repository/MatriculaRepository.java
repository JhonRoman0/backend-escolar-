package com.example.Escolar.Repository;

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
    List<Matricula> findByAccesoNot(Byte acceso);

    Page<Matricula> findByAccesoNot(Byte acceso, Pageable pageable);

    Optional<Matricula> findByIdMatriculaAndAccesoNot(Integer idMatricula, Byte acceso);

    Optional<Matricula> findByAlumnoApoderadoAndAccesoNot(AlumnoApoderado alumnoApoderado, Byte acceso);

    Optional<Matricula> findByAlumnoApoderadoAndAcceso(AlumnoApoderado alumnoApoderado, Byte acceso);

    boolean existsByAlumnoApoderado(AlumnoApoderado alumnoApoderado);

    List<Matricula> findByGradoSeccionIdGradoSeccionAndAccesoNot(Integer idGradoSeccion, Byte acceso);

    List<Matricula> findByFechaRegistroBetweenAndAccesoNot(LocalDate inicio, LocalDate fin, Byte acceso);

    @Query("SELECT m FROM Matricula m " +
            "JOIN FETCH m.alumnoApoderado aa " +
            "JOIN FETCH aa.alumno a " +
            "WHERE a.idAlumno = :idAlumno AND m.acceso = :acceso")
    List<Matricula> findActivasPorAlumnoId(@Param("idAlumno") Integer idAlumno, @Param("acceso") Byte acceso);
}