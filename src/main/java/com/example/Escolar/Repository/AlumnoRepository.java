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

    Optional<Alumno> findByIdAlumnoAndAccesoNot(Integer idAlumno, Acceso acceso);

    Optional<Alumno> findByCodigo(String codigo);

    Optional<Alumno> findByCodigoAndAccesoNot(String codigo, Acceso acceso);

    Optional<Alumno> findByCodigoHashAndAccesoNot(String codigoHash, Acceso acceso);

    Optional<Alumno> findByDocumentoIdentidadAndAccesoNot(String documentoIdentidad, Acceso acceso);

    long countByCodigoStartingWith(String prefijo);

    List<Alumno> findByFechaIngresoBetweenAndAccesoNot(LocalDate inicio, LocalDate fin, Acceso acceso);
}