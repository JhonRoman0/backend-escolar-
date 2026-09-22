package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Asignacion;
import com.example.Escolar.Model.CambioDocente;
import com.example.Escolar.Model.Docente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CambioDocenteRepository extends JpaRepository<CambioDocente, Integer> {

    List<CambioDocente> findByAccesoNot(Acceso acceso);

    List<CambioDocente> findByDocenteAnteriorAndAccesoNot(Docente docenteAnterior, Acceso acceso);

    List<CambioDocente> findByAsignacionAndAccesoNot(Asignacion asignacion, Acceso acceso);
}
