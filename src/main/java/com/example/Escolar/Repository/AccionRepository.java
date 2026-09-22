package com.example.Escolar.Repository;

import com.example.Escolar.Model.Accion;
import com.example.Escolar.Model.Acceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccionRepository extends JpaRepository<Accion, Integer> {

    List<Accion> findByAccesoNot(Acceso acceso);

    Optional<Accion> findByIdAccionAndAccesoNot(Integer idAccion, Acceso acceso);

    Optional<Accion> findByCodigoAndAccesoNot(String codigo, Acceso acceso);
}
