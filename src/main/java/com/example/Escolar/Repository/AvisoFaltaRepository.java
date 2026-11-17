package com.example.Escolar.Repository;

import com.example.Escolar.Model.AvisoFalta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AvisoFaltaRepository extends JpaRepository<AvisoFalta, Integer> {
    List<AvisoFalta> findByFecha(LocalDate fecha);
}
