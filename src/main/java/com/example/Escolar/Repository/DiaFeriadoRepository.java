package com.example.Escolar.Repository;

import com.example.Escolar.Model.DiaFeriado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiaFeriadoRepository extends JpaRepository<DiaFeriado, Integer> {

    List<DiaFeriado> findByAccesoNot(Byte acceso);

    Optional<DiaFeriado> findByIdDiaFeriadoAndAccesoNot(Integer idDiaFeriado, Byte acceso);

    List<DiaFeriado> findByFechaBetweenAndAccesoNot(LocalDate inicio, LocalDate fin, Byte acceso);
}