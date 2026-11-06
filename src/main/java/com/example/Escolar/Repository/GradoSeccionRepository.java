package com.example.Escolar.Repository;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Grado;
import com.example.Escolar.Model.GradoSeccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GradoSeccionRepository extends JpaRepository<GradoSeccion, Integer> {
    List<GradoSeccion> findByGradoAndAccesoNot(Grado grado, Acceso acceso);

    List<GradoSeccion> findByGradoIdGradoAndTurnoIdTurnoAndAnioEscolarIdAnioAndAccesoNot(
            Integer idGrado, Integer idTurno, Integer idAnio, Acceso acceso);

    Optional<GradoSeccion> findByIdGradoSeccionAndAccesoNot(Integer idGradoSeccion, Acceso acceso);

    long countByAnioEscolarIdAnioAndAccesoNot(Integer idAnio, Acceso acceso);

    List<GradoSeccion> findByAnioEscolarIdAnioAndAccesoNot(Integer idAnio, Acceso acceso);

    // Bloquea la sección mientras se cuenta su cupo: dos matrículas no toman el último lugar a la vez.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT gs FROM GradoSeccion gs WHERE gs.idGradoSeccion = :id")
    Optional<GradoSeccion> bloquear(@org.springframework.data.repository.query.Param("id") Integer id);

    long countByTurnoIdTurnoAndAccesoNot(Integer idTurno, Acceso acceso);

    /** Secciones que tienen esta aula como salon. */
    List<GradoSeccion> findByAulaIdAulaAndAccesoNot(Integer idAula, Acceso acceso);
}