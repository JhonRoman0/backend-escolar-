package com.example.Escolar.Repository;

import com.example.Escolar.Model.Apoderado;
import com.example.Escolar.Model.Acceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApoderadoRepository extends JpaRepository<Apoderado, Integer> {

    List<Apoderado> findByAccesoNot(Acceso acceso);

    Optional<Apoderado> findByIdApoderadoAndAccesoNot(Integer idApoderado, Acceso acceso);

    Optional<Apoderado> findByUsuarioDocumentoIdentidadAndAccesoNot(String documentoIdentidad, Acceso acceso);

    Optional<Apoderado> findByUsuarioIdUsuarioAndAccesoNot(Integer idUsuario, Acceso acceso);

    Optional<Apoderado> findByUsuarioIdUsuario(Integer idUsuario);
}