package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.DocenteNivel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocenteNivelRepository extends JpaRepository<DocenteNivel, Integer> {
    List<DocenteNivel> findByDocenteAndAccesoNot(Docente docente, Acceso acceso);
    List<DocenteNivel> findByDocente(Docente docente);
    void deleteByDocente(Docente docente);
}
