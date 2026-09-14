package com.example.Escolar.Repository;

import com.example.Escolar.Model.ContactoMensaje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContactoMensajeRepository extends JpaRepository<ContactoMensaje, Integer> {
    List<ContactoMensaje> findByAccesoNot(Byte acceso);

    Optional<ContactoMensaje> findByIdMensajeAndAccesoNot(Integer idMensaje, Byte acceso);
}