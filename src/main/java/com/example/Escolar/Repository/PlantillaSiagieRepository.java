package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.PlantillaSiagie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlantillaSiagieRepository extends JpaRepository<PlantillaSiagie, Integer> {
    List<PlantillaSiagie> findByAccesoNot(Acceso acceso);

    Optional<PlantillaSiagie> findByIdPlantillaAndAccesoNot(Integer id, Acceso acceso);

    Optional<PlantillaSiagie> findByVigenteAndAccesoNot(byte vigente, Acceso acceso);

    List<PlantillaSiagie> findByAnioAndAccesoNot(String anio, Acceso acceso);
}
