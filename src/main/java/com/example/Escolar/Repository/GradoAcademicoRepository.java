package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.GradoAcademico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GradoAcademicoRepository extends JpaRepository<GradoAcademico, Integer> {
    Optional<GradoAcademico> findByIdGradoAcademicoAndAccesoNot(Integer id, Acceso acceso);
    Optional<GradoAcademico> findByNombreAndAccesoNot(String nombre, Acceso acceso);
    List<GradoAcademico> findByAccesoNot(Acceso acceso);
}
