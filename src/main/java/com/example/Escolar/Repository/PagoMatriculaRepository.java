package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.PagoMatricula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoMatriculaRepository extends JpaRepository<PagoMatricula, Integer> {
    List<PagoMatricula> findByMatriculaIdMatriculaAndAccesoNotOrderByFecha(Integer idMatricula, Acceso acceso);

    boolean existsByMetodoPagoIdMetodoPago(Integer idMetodoPago);
}
