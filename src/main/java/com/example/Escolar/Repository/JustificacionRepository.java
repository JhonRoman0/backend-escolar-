package com.example.Escolar.Repository;

import com.example.Escolar.Model.Justificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JustificacionRepository extends JpaRepository<Justificacion, Integer> {

    List<Justificacion> findByAccesoNot(Byte acceso);

    Optional<Justificacion> findByIdJustificacionAndAccesoNot(Integer idJustificacion, Byte acceso);
}