package com.example.Escolar.Repository;

import com.example.Escolar.Model.Colegio;
import com.example.Escolar.Model.Acceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ColegioRepository extends JpaRepository<Colegio, Integer> {
    List<Colegio> findByAccesoNot(Acceso acceso);

    Optional<Colegio> findByIdColegioAndAccesoNot(Integer idColegio, Acceso acceso);

    Optional<Colegio> findFirstByAccesoNot(Acceso acceso);
}