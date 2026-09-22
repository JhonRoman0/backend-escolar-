package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Nota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NotaRepository extends JpaRepository<Nota, Integer> {
    List<Nota> findByAccesoNot(Acceso acceso);

    List<Nota> findByCompetenciaIdCompetenciaAndBimestreAndAccesoNot(Integer idCompetencia, Byte bimestre, Acceso acceso);

    List<Nota> findByMatriculaIdMatriculaAndAccesoNot(Integer idMatricula, Acceso acceso);

    List<Nota> findByMatriculaIdMatriculaAndBimestreAndAccesoNot(Integer idMatricula, Byte bimestre, Acceso acceso);

    Optional<Nota> findByMatriculaIdMatriculaAndCompetenciaIdCompetenciaAndBimestreAndAccesoNot(
            Integer idMatricula, Integer idCompetencia, Byte bimestre, Acceso acceso);

    List<Nota> findByFechaRegistroBetweenAndAccesoNot(LocalDate inicio, LocalDate fin, Acceso acceso);

    List<Nota> findByMatriculaGradoSeccionIdGradoSeccionAndBimestreAndAccesoNot(
            Integer idGradoSeccion, Byte bimestre, Acceso acceso);
}
