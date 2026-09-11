package com.example.Escolar.Repository;

import com.example.Escolar.Model.Apoderado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApoderadoRepository extends JpaRepository<Apoderado, Integer> {

    List<Apoderado> findByAccesoNot(Byte acceso);

    Optional<Apoderado> findByIdApoderadoAndAccesoNot(Integer idApoderado, Byte acceso);

    Optional<Apoderado> findByUsuarioDocumentoIdentidadAndAccesoNot(String documentoIdentidad, Byte acceso);

    Optional<Apoderado> findByUsuarioIdUsuarioAndAccesoNot(Integer idUsuario, Byte acceso);
}