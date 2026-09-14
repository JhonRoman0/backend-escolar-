package com.example.Escolar.Repository;

import com.example.Escolar.Model.GaleriaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GaleriaDetalleRepository extends JpaRepository<GaleriaDetalle, Integer> {
    List<GaleriaDetalle> findByGaleriaIdGaleriaAndAccesoNot(Integer idGaleria, Byte acceso);

    List<GaleriaDetalle> findByGaleriaIdGaleria(Integer idGaleria);
}