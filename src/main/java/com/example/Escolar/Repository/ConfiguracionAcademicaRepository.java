package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.ConfiguracionAcademica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConfiguracionAcademicaRepository extends JpaRepository<ConfiguracionAcademica, Integer> {
    Optional<ConfiguracionAcademica> findByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(Integer idAnio, Integer idNivel, Acceso acceso);

    /** Las de un nivel en todos los años: de aqui se hereda la del año anterior. */
    List<ConfiguracionAcademica> findByNivelIdNivelAndAccesoNot(Integer idNivel, Acceso acceso);
}
