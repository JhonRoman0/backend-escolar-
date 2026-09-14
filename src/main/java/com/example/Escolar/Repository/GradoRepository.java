package com.example.Escolar.Repository;

import com.example.Escolar.Model.Grado;
import com.example.Escolar.Model.Nivel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GradoRepository extends JpaRepository<Grado, Integer> {
    List<Grado> findByAccesoNot(Byte acceso);

    Optional<Grado> findByIdGradoAndAccesoNot(Integer idGrado, Byte acceso);

    List<Grado> findByNombreAndAccesoNot(String nombre, Byte acceso);

    List<Grado> findByNivelAndAccesoNot(Nivel nivel, Byte acceso);
}