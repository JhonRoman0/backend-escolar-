package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Nivel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NivelRepository extends JpaRepository<Nivel, Integer> {
    Optional<Nivel> findByIdNivelAndAccesoNot(Integer id, Acceso acceso);
    Optional<Nivel> findByNombreAndAccesoNot(String nombre, Acceso acceso);
    List<Nivel> findByAccesoNot(Acceso acceso);
}
