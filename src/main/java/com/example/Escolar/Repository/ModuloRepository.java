package com.example.Escolar.Repository;

import com.example.Escolar.Model.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModuloRepository extends JpaRepository<Modulo, Integer> {

    List<Modulo> findByAccesoNot(Byte acceso);

    Optional<Modulo> findByIdModuloAndAccesoNot(Integer idModulo, Byte acceso);

    Optional<Modulo> findByModuloAndAccesoNot(String modulo, Byte acceso);
}
