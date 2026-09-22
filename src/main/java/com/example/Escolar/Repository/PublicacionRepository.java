package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Publicacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PublicacionRepository extends JpaRepository<Publicacion, Integer> {
    List<Publicacion> findByAccesoNot(Acceso acceso);

    List<Publicacion> findByEstadoAndAccesoNot(Byte estado, Acceso acceso);

    Optional<Publicacion> findByIdPublicacionAndAccesoNot(Integer idPublicacion, Acceso acceso);
}