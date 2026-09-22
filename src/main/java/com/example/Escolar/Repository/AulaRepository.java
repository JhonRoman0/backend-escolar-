package com.example.Escolar.Repository;

import com.example.Escolar.Model.Aula;
import com.example.Escolar.Model.Acceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AulaRepository extends JpaRepository<Aula, Integer> {
    List<Aula> findByAccesoNot(Acceso acceso);

    Optional<Aula> findByIdAulaAndAccesoNot(Integer idAula, Acceso acceso);

    Optional<Aula> findByNombreAndAccesoNot(String nombre, Acceso acceso);
}
