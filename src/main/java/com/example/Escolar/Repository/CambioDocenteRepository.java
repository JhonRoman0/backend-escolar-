package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.CambioDocente;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.EstadoCambioDocente;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CambioDocenteRepository extends JpaRepository<CambioDocente, Integer> {

    List<CambioDocente> findByAccesoNot(Acceso acceso);

    Optional<CambioDocente> findBySuspensionIdSuspension(Integer idSuspension);

    List<CambioDocente> findBySuspensionIdSuspensionIn(Collection<Integer> idsSuspension);

    List<CambioDocente> findByDocenteAnteriorAndAccesoNot(Docente docenteAnterior, Acceso acceso);

    List<CambioDocente> findByEstadoInAndAccesoNot(Collection<EstadoCambioDocente> estados, Acceso acceso);

    boolean existsByAnioEscolarIdAnioAndEstadoInAndAccesoNot(
            Integer idAnio, Collection<EstadoCambioDocente> estados, Acceso acceso);

    boolean existsByAnioEscolarIdAnioAndIncidenciaTrueAndAccesoNot(Integer idAnio, Acceso acceso);

    List<CambioDocente> findByEstadoAndFechaInicioLessThanEqualAndAccesoNot(
            EstadoCambioDocente estado, LocalDate fecha, Acceso acceso);

    List<CambioDocente> findByEstadoAndIncidenciaFalseAndFechaFinLessThanAndAccesoNot(
            EstadoCambioDocente estado, LocalDate fecha, Acceso acceso);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CambioDocente c WHERE c.idCambio = :id")
    Optional<CambioDocente> bloquear(@Param("id") Integer id);
}
