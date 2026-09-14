package com.example.Escolar.Repository;

import com.example.Escolar.Model.Colegio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ColegioRepository extends JpaRepository<Colegio, Integer> {
    List<Colegio> findByAccesoNot(Byte acceso);

    Optional<Colegio> findByIdColegioAndAccesoNot(Integer idColegio, Byte acceso);

    Optional<Colegio> findFirstByAccesoNot(Byte acceso);
}