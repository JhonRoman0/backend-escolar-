package com.example.Escolar.Repository;

import com.example.Escolar.Model.PlantillaSiagie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlantillaSiagieRepository extends JpaRepository<PlantillaSiagie, Integer> {
    List<PlantillaSiagie> findByAccesoNot(Byte acceso);

    Optional<PlantillaSiagie> findByIdPlantillaAndAccesoNot(Integer id, Byte acceso);

    Optional<PlantillaSiagie> findByVigenteAndAccesoNot(byte vigente, Byte acceso);

    List<PlantillaSiagie> findByAnioAndAccesoNot(String anio, Byte acceso);
}
