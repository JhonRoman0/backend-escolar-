package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Turno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TurnoRepository extends JpaRepository<Turno, Integer> {
    List<Turno> findByAccesoNot(Acceso acceso);

    Optional<Turno> findByIdTurnoAndAccesoNot(Integer idTurno, Acceso acceso);

    Optional<Turno> findByNombreAndAccesoNot(String nombre, Acceso acceso);
}