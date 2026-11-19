package com.example.Escolar.Repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import com.example.Escolar.Dto.MarcaAsistencia;
import com.example.Escolar.Model.AsistenciaAlumno;
import com.example.Escolar.Model.Acceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AsistenciaAlumnoRepository extends JpaRepository<AsistenciaAlumno, Integer> {

    List<AsistenciaAlumno> findByAccesoNot(Acceso acceso);

    Optional<AsistenciaAlumno> findByIdAsistenciaAndAccesoNot(Integer idAsistencia, Acceso acceso);

    Optional<AsistenciaAlumno> findByMatriculaIdMatriculaAndFechaAndAccesoNot(Integer idMatricula, LocalDate fecha, Acceso acceso);

    boolean existsByMatriculaIdMatriculaAndFechaAndAccesoNot(Integer idMatricula, LocalDate fecha, Acceso acceso);

    List<AsistenciaAlumno> findByFechaBetweenAndAccesoNot(LocalDate inicio, LocalDate fin, Acceso acceso);

    List<AsistenciaAlumno> findByFechaBetweenAndAccesoNotOrderByFechaAscHoraEntradaAsc(LocalDate inicio, LocalDate fin, Acceso acceso);

    List<AsistenciaAlumno> findByMatriculaIdMatriculaAndFechaBetweenAndAccesoNot(Integer idMatricula, LocalDate inicio, LocalDate fin, Acceso acceso);

    List<AsistenciaAlumno> findByMatriculaIdMatriculaInAndFechaBetweenAndAccesoNot(java.util.Collection<Integer> idsMatricula,
            LocalDate inicio, LocalDate fin, Acceso acceso);
    List<AsistenciaAlumno> findByUsuarioRegistroIdUsuarioAndFechaBetweenAndAccesoNot(Integer idUsuario, LocalDate inicio, LocalDate fin, Acceso acceso);

    /** Marcas de varias matrículas en un rango, solo con las columnas que usan los listados. */
    @Query("select new com.example.Escolar.Dto.MarcaAsistencia(a.matricula.idMatricula, a.idAsistencia, a.fecha, a.horaEntrada, "
            + "e.nombre, j.motivo, a.acompanante, a.urlEvidencia, u.nombre, u.apellidoPat) "
            + "from AsistenciaAlumno a join a.estado e left join a.justificacion j join a.usuarioRegistro u "
            + "where a.matricula.idMatricula in :ids and a.fecha between :inicio and :fin and a.acceso <> :eliminado")
    List<MarcaAsistencia> marcas(@Param("ids") java.util.Collection<Integer> ids, @Param("inicio") LocalDate inicio,
                                 @Param("fin") LocalDate fin, @Param("eliminado") Acceso eliminado);
}
