package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Justificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JustificacionRepository extends JpaRepository<Justificacion, Integer> {

    List<Justificacion> findByAccesoNot(Acceso acceso);

    Optional<Justificacion> findByIdJustificacionAndAccesoNot(Integer idJustificacion, Acceso acceso);
}