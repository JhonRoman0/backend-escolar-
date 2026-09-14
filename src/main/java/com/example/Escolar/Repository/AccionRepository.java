package com.example.Escolar.Repository;

import com.example.Escolar.Model.Accion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccionRepository extends JpaRepository<Accion, Integer> {

    List<Accion> findByAccesoNot(Byte acceso);

    Optional<Accion> findByIdAccionAndAccesoNot(Integer idAccion, Byte acceso);

    Optional<Accion> findByCodigoAndAccesoNot(String codigo, Byte acceso);
}
