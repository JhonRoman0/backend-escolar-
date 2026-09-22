package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.HistorialMatricula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HistorialMatriculaRepository extends JpaRepository<HistorialMatricula, Integer> {
    List<HistorialMatricula> findByMatriculaIdMatriculaAndAccesoNot(Integer idMatricula, Acceso acceso);

    Optional<HistorialMatricula> findByIdHistorialAndAccesoNot(Integer idHistorial, Acceso acceso);
}