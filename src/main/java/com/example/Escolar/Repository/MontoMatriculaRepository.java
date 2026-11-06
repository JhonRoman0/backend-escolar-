package com.example.Escolar.Repository;

import com.example.Escolar.Model.MontoMatricula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MontoMatriculaRepository extends JpaRepository<MontoMatricula, Integer> {
    List<MontoMatricula> findByAnioEscolarIdAnio(Integer idAnio);

    Optional<MontoMatricula> findByNivelIdNivelAndAnioEscolarIdAnio(Integer idNivel, Integer idAnio);
}
