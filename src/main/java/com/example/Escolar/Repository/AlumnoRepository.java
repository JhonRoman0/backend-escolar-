package com.example.Escolar.Repository;

import com.example.Escolar.Model.Alumno;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AlumnoRepository extends JpaRepository<Alumno, Integer> {

    List<Alumno> findByAccesoNot(Byte acceso);

    Page<Alumno> findByAccesoNot(Byte acceso, Pageable pageable);

    Optional<Alumno> findByIdAlumnoAndAccesoNot(Integer idAlumno, Byte acceso);

    Optional<Alumno> findByCodigo(String codigo);

    Optional<Alumno> findByCodigoAndAccesoNot(String codigo, Byte acceso);

    Optional<Alumno> findByCodigoHashAndAccesoNot(String codigoHash, Byte acceso);

    Optional<Alumno> findByDocumentoIdentidadAndAccesoNot(String documentoIdentidad, Byte acceso);

    long countByCodigoStartingWith(String prefijo);

    List<Alumno> findByFechaIngresoBetweenAndAccesoNot(LocalDate inicio, LocalDate fin, Byte acceso);
}