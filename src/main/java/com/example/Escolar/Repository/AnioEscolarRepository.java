package com.example.Escolar.Repository;

import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.Acceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnioEscolarRepository extends JpaRepository<AnioEscolar, Integer> {
    List<AnioEscolar> findByAccesoNot(Acceso acceso);

    Optional<AnioEscolar> findByIdAnioAndAccesoNot(Integer idAnio, Acceso acceso);

    Optional<AnioEscolar> findByAnioAndAccesoNot(String anio, Acceso acceso);

    Optional<AnioEscolar> findByEstadoAndAccesoNot(Byte estado, Acceso acceso);
}