package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.SuspensionDocente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SuspensionDocenteRepository extends JpaRepository<SuspensionDocente, Integer> {

    List<SuspensionDocente> findByAccesoNot(Acceso acceso);

    List<SuspensionDocente> findByDocenteAndAccesoNot(Docente docente, Acceso acceso);

    Optional<SuspensionDocente> findByIdSuspensionAndAccesoNot(Integer idSuspension, Acceso acceso);

    boolean existsByDocenteAndAcceso(Docente docente, Acceso acceso);
}
