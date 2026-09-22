package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Model.Recreo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecreoRepository extends JpaRepository<Recreo, Integer> {

    List<Recreo> findByAccesoNot(Acceso acceso);

    List<Recreo> findByNivelAndAccesoNot(Nivel nivel, Acceso acceso);

    List<Recreo> findByNivelAndGradoSeccionAndAccesoNot(Nivel nivel, GradoSeccion gradoSeccion, Acceso acceso);

    List<Recreo> findByNivelAndDiaSemanaAndAccesoNot(Nivel nivel, Byte diaSemana, Acceso acceso);
}
