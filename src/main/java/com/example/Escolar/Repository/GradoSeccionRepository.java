package com.example.Escolar.Repository;

import com.example.Escolar.Model.Grado;
import com.example.Escolar.Model.GradoSeccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GradoSeccionRepository extends JpaRepository<GradoSeccion, Integer> {
    List<GradoSeccion> findByGradoAndAccesoNot(Grado grado, Byte acceso);

    Optional<GradoSeccion> findByIdGradoSeccionAndAccesoNot(Integer idGradoSeccion, Byte acceso);
}