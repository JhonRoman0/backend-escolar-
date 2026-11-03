package com.example.Escolar.Repository;

import com.example.Escolar.Model.Alumno;
import com.example.Escolar.Model.AlumnoApoderado;
import com.example.Escolar.Model.Apoderado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlumnoApoderadoRepository extends JpaRepository<AlumnoApoderado, Integer> {

    /** Solo los vínculos activos: un apoderado desvinculado ya no ve al alumno. */
    @Query("SELECT v FROM AlumnoApoderado v WHERE v.alumno = :alumno AND v.activo = true")
    List<AlumnoApoderado> findByAlumno(@Param("alumno") Alumno alumno);

    @Query("SELECT v FROM AlumnoApoderado v WHERE v.apoderado = :apoderado AND v.activo = true")
    List<AlumnoApoderado> findByApoderado(@Param("apoderado") Apoderado apoderado);

    /** También los inactivos: para reactivar un vínculo en vez de duplicarlo. */
    List<AlumnoApoderado> findTodosByAlumno(Alumno alumno);
}