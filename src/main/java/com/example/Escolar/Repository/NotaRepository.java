package com.example.Escolar.Repository;

import com.example.Escolar.Model.Nota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NotaRepository extends JpaRepository<Nota, Integer> {
    List<Nota> findByAccesoNot(Byte acceso);

    List<Nota> findByCompetenciaIdCompetenciaAndBimestreAndAccesoNot(Integer idCompetencia, Byte bimestre, Byte acceso);

    List<Nota> findByMatriculaIdMatriculaAndAccesoNot(Integer idMatricula, Byte acceso);

    List<Nota> findByMatriculaIdMatriculaAndBimestreAndAccesoNot(Integer idMatricula, Byte bimestre, Byte acceso);

    Optional<Nota> findByMatriculaIdMatriculaAndCompetenciaIdCompetenciaAndBimestreAndAccesoNot(
            Integer idMatricula, Integer idCompetencia, Byte bimestre, Byte acceso);

    List<Nota> findByFechaRegistroBetweenAndAccesoNot(LocalDate inicio, LocalDate fin, Byte acceso);

    List<Nota> findByMatriculaGradoSeccionIdGradoSeccionAndBimestreAndAccesoNot(
            Integer idGradoSeccion, Byte bimestre, Byte acceso);
}
