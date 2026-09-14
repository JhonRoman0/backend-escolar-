package com.example.Escolar.Repository;

import com.example.Escolar.Model.AjustePortal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AjustePortalRepository extends JpaRepository<AjustePortal, Integer> {
    List<AjustePortal> findByAccesoNot(Byte acceso);

    Optional<AjustePortal> findByIdAjusteAndAccesoNot(Integer idAjuste, Byte acceso);

    Optional<AjustePortal> findByClaveAndAccesoNot(String clave, Byte acceso);
}