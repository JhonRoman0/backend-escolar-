package com.example.Escolar.Repository;

import com.example.Escolar.Model.AnioEscolar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnioEscolarRepository extends JpaRepository<AnioEscolar, Integer> {
    List<AnioEscolar> findByAccesoNot(Byte acceso);

    Optional<AnioEscolar> findByIdAnioAndAccesoNot(Integer idAnio, Byte acceso);

    Optional<AnioEscolar> findByAnioAndAccesoNot(String anio, Byte acceso);

    Optional<AnioEscolar> findByEstadoAndAccesoNot(Byte estado, Byte acceso);
}