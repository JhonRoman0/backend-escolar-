package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Grado;
import com.example.Escolar.Model.Nivel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GradoRepository extends JpaRepository<Grado, Integer> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select g from Grado g where g.idGrado = :id")
    Optional<Grado> bloquearPorId(@org.springframework.data.repository.query.Param("id") Integer id);

    List<Grado> findByAccesoNot(Acceso acceso);

    Optional<Grado> findByIdGradoAndAccesoNot(Integer idGrado, Acceso acceso);

    List<Grado> findByNombreAndAccesoNot(String nombre, Acceso acceso);

    Optional<Grado> findByNombreAndNivelAndAccesoNot(String nombre, Nivel nivel, Acceso acceso);

    List<Grado> findByNivelAndAccesoNot(Nivel nivel, Acceso acceso);
}