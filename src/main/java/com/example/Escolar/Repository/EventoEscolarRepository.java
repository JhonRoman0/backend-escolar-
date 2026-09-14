package com.example.Escolar.Repository;

import com.example.Escolar.Model.EventoEscolar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventoEscolarRepository extends JpaRepository<EventoEscolar, Integer> {
    List<EventoEscolar> findByAccesoNot(Byte acceso);

    List<EventoEscolar> findByEsPublicoAndAccesoNot(Byte esPublico, Byte acceso);

    Optional<EventoEscolar> findByIdEventoAndAccesoNot(Integer idEvento, Byte acceso);
}