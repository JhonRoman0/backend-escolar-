package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.GaleriaImagen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GaleriaImagenRepository extends JpaRepository<GaleriaImagen, Integer> {
    List<GaleriaImagen> findByAccesoNot(Acceso acceso);

    Optional<GaleriaImagen> findByIdGaleriaAndAccesoNot(Integer idGaleria, Acceso acceso);
}