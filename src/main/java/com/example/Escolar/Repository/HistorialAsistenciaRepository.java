package com.example.Escolar.Repository;

import com.example.Escolar.Model.HistorialAsistencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialAsistenciaRepository extends JpaRepository<HistorialAsistencia, Integer> {

    List<HistorialAsistencia> findByAsistenciaAlumnoIdAsistenciaAndAccesoNot(Integer idAsistencia, Byte acceso);
}