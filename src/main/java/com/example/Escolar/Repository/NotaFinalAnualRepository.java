package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.NotaFinalAnual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface NotaFinalAnualRepository extends JpaRepository<NotaFinalAnual, Integer> {
    List<NotaFinalAnual> findByMatriculaIdMatriculaInAndCompetenciaIdCompetenciaInAndAccesoNot(
            Collection<Integer> idsMatricula, Collection<Integer> idsCompetencia, Acceso acceso);

    List<NotaFinalAnual> findByMatriculaGradoSeccionIdGradoSeccionAndAccesoNot(Integer idGradoSeccion, Acceso acceso);

    List<NotaFinalAnual> findByMatriculaIdMatriculaAndAccesoNot(Integer idMatricula, Acceso acceso);

    /** Si la sección ya usó notas del año (los años previos a ellas no tienen ninguna). */
    boolean existsByMatriculaGradoSeccionIdGradoSeccionAndAccesoNot(Integer idGradoSeccion, Acceso acceso);
}
