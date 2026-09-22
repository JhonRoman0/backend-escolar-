package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Seccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeccionRepository extends JpaRepository<Seccion, Integer> {
    List<Seccion> findByAccesoNot(Acceso acceso);

    Optional<Seccion> findByIdSeccionAndAccesoNot(Integer idSeccion, Acceso acceso);
}