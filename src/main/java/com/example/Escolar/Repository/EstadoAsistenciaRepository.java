package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.EstadoAsistencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstadoAsistenciaRepository extends JpaRepository<EstadoAsistencia, Integer> {

    List<EstadoAsistencia> findByAccesoNot(Acceso acceso);

    Optional<EstadoAsistencia> findByIdEstadoAndAccesoNot(Integer idEstado, Acceso acceso);

    Optional<EstadoAsistencia> findByNombreIgnoreCaseAndAccesoNot(String nombre, Acceso acceso);
}