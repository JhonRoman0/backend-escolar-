package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.TipoContrato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoContratoRepository extends JpaRepository<TipoContrato, Integer> {
    Optional<TipoContrato> findByIdTipoContratoAndAccesoNot(Integer id, Acceso acceso);
    Optional<TipoContrato> findByNombreAndAccesoNot(String nombre, Acceso acceso);
    List<TipoContrato> findByAccesoNot(Acceso acceso);
}
