package com.example.Escolar.Repository;

import com.example.Escolar.Model.Alumno;
import com.example.Escolar.Model.AlumnoApoderado;
import com.example.Escolar.Model.Apoderado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlumnoApoderadoRepository extends JpaRepository<AlumnoApoderado, Integer> {

    List<AlumnoApoderado> findByAlumno(Alumno alumno);

    List<AlumnoApoderado> findByApoderado(Apoderado apoderado);
}