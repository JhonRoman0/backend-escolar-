package com.example.Escolar.Repository;

import com.example.Escolar.Model.Docente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DocenteRepository extends JpaRepository<Docente, Integer> {

    List<Docente> findByAccesoNot(Byte acceso);

    Optional<Docente> findByIdDocenteAndAccesoNot(Integer idDocente, Byte acceso);

    Optional<Docente> findByUsuarioIdUsuario(Integer idUsuario);

    List<Docente> findByFechaContratacionBetweenAndAccesoNot(LocalDate inicio, LocalDate fin, Byte acceso);
}
