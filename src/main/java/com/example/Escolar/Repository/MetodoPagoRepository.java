package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetodoPagoRepository extends JpaRepository<MetodoPago, Integer> {
    List<MetodoPago> findByAccesoNotOrderByNombre(Acceso acceso);

    Optional<MetodoPago> findByIdMetodoPagoAndAccesoNot(Integer id, Acceso acceso);

    Optional<MetodoPago> findByNombreIgnoreCaseAndAccesoNot(String nombre, Acceso acceso);
}
