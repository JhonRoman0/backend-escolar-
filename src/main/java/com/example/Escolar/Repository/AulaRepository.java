package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Aula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AulaRepository extends JpaRepository<Aula, Integer> {
    List<Aula> findByAccesoNot(Acceso acceso);

    Optional<Aula> findByIdAulaAndAccesoNot(Integer idAula, Acceso acceso);

    @Query("SELECT a FROM Aula a WHERE LOWER(TRIM(a.nombre)) = LOWER(TRIM(:nombre))")
    Optional<Aula> findByNombreIgnoreCase(@Param("nombre") String nombre);

    @Query("SELECT a FROM Aula a WHERE LOWER(TRIM(a.nombre)) = LOWER(TRIM(:nombre)) AND a.idAula <> :idExcluir")
    Optional<Aula> findByNombreIgnoreCaseAndIdAulaNot(@Param("nombre") String nombre, @Param("idExcluir") Integer idExcluir);
}
