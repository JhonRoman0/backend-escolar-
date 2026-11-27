package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.MotivoCambioDocente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MotivoCambioDocenteRepository extends JpaRepository<MotivoCambioDocente, Integer> {

    List<MotivoCambioDocente> findByAccesoNot(Acceso acceso);

    List<MotivoCambioDocente> findByAplicaTemporalTrueAndAccesoNot(Acceso acceso);

    List<MotivoCambioDocente> findByAplicaDefinitivoTrueAndAccesoNot(Acceso acceso);

    Optional<MotivoCambioDocente> findByIdMotivoCambioDocenteAndAccesoNot(Integer id, Acceso acceso);

    Optional<MotivoCambioDocente> findByIdMotivoCambioDocenteAndAcceso(Integer id, Acceso acceso);

    boolean existsByNombreIgnoreCaseAndAccesoNot(String nombre, Acceso acceso);
}
