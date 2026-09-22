package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.NotaAutorizacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotaAutorizacionRepository extends JpaRepository<NotaAutorizacion, Integer> {
    Optional<NotaAutorizacion> findByIdNotaAutorizacionAndAccesoNot(Integer id, Acceso acceso);

    Optional<NotaAutorizacion> findByCodigoHashAndAccesoNot(String codigoHash, Acceso acceso);
}