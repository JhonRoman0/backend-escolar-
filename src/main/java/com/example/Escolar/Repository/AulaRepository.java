package com.example.Escolar.Repository;

import com.example.Escolar.Model.Aula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AulaRepository extends JpaRepository<Aula, Integer> {
    List<Aula> findByAccesoNot(Byte acceso);

    Optional<Aula> findByIdAulaAndAccesoNot(Integer idAula, Byte acceso);

    Optional<Aula> findByNombreAndAccesoNot(String nombre, Byte acceso);
}
