package com.example.Escolar.Repository;

import com.example.Escolar.Model.Turno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TurnoRepository extends JpaRepository<Turno, Integer> {
    List<Turno> findByAccesoNot(Byte acceso);

    Optional<Turno> findByIdTurnoAndAccesoNot(Integer idTurno, Byte acceso);

    Optional<Turno> findByNombreAndAccesoNot(String nombre, Byte acceso);
}