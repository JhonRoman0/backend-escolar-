package com.example.Escolar.Repository;

import com.example.Escolar.Config.CacheConfig;
import com.example.Escolar.Model.Acceso;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccesoRepository extends JpaRepository<Acceso, Long> {

    /** Activo, Eliminado e Inactivo son fijos: se leen una sola vez. */
    @Override
    @Cacheable(cacheNames = CacheConfig.ACCESOS, unless = "#result == null")
    Optional<Acceso> findById(Long id);
}
