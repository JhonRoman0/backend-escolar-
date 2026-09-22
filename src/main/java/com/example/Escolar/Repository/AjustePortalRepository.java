package com.example.Escolar.Repository;

import com.example.Escolar.Model.AjustePortal;
import com.example.Escolar.Model.Acceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AjustePortalRepository extends JpaRepository<AjustePortal, Integer> {
    List<AjustePortal> findByAccesoNot(Acceso acceso);

    Optional<AjustePortal> findByIdAjusteAndAccesoNot(Integer idAjuste, Acceso acceso);

    Optional<AjustePortal> findByClaveAndAccesoNot(String clave, Acceso acceso);
}