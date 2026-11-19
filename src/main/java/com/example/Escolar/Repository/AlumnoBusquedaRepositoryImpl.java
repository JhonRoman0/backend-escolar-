package com.example.Escolar.Repository;

import com.example.Escolar.Model.Alumno;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class AlumnoBusquedaRepositoryImpl implements AlumnoBusquedaRepository {

    private static final Set<String> SORT_PERMITIDOS = Set.of(
            "idAlumno", "nombre", "apellidoPat", "apellidoMat", "codigo", "fechaIngreso");

    private static final String LIKE_ESCAPE = "!";

    private final EntityManager entityManager;

    public AlumnoBusquedaRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<Alumno> buscar(
            List<String> tokens,
            Long accesoActivoId,
            Long eliminadoId,
            Integer idNivel,
            Integer idGrado,
            Integer idSeccion,
            Integer idTurno,
            Integer idGradoSeccion,
            Integer idAnio,
            Collection<Integer> idsVisibles,
            Pageable pageable) {
        boolean conFiltrosAcademicos = idNivel != null || idGrado != null || idSeccion != null
                || idTurno != null || idGradoSeccion != null || idAnio != null;

        StringBuilder desde = new StringBuilder(" FROM Alumno a ");
        if (conFiltrosAcademicos) {
            desde.append("JOIN AlumnoApoderado aa ON aa.alumno = a ")
                    .append("JOIN Matricula m ON m.alumnoApoderado = aa AND m.acceso.idAcceso = :accesoActivoId ")
                    .append("JOIN m.gradoSeccion gs ")
                    .append("JOIN gs.grado g JOIN g.nivel n LEFT JOIN gs.seccion s JOIN gs.turno t JOIN gs.anioEscolar anio ")
                    .append("WHERE a.acceso.idAcceso <> :eliminadoId AND gs.acceso.idAcceso <> :eliminadoId ");
        } else {
            desde.append("WHERE a.acceso.idAcceso <> :eliminadoId ");
        }

        List<String> condiciones = new ArrayList<>();
        if (conFiltrosAcademicos) {
            if (idNivel != null) condiciones.add("n.idNivel = :idNivel");
            if (idGrado != null) condiciones.add("g.idGrado = :idGrado");
            if (idSeccion != null) condiciones.add("s.idSeccion = :idSeccion");
            if (idTurno != null) condiciones.add("t.idTurno = :idTurno");
            if (idGradoSeccion != null) condiciones.add("gs.idGradoSeccion = :idGradoSeccion");
            if (idAnio != null) condiciones.add("anio.idAnio = :idAnio");
        }
        if (idsVisibles != null) condiciones.add("a.idAlumno IN :visibles");
        String escapeLike = " ESCAPE '" + LIKE_ESCAPE + "'";
        for (int i = 0; i < tokens.size(); i++) {
            String parametro = ":t" + i + escapeLike;
            condiciones.add("(LOWER(a.nombre) LIKE " + parametro
                    + " OR LOWER(a.apellidoPat) LIKE " + parametro
                    + " OR LOWER(a.apellidoMat) LIKE " + parametro
                    + " OR LOWER(a.codigo) LIKE " + parametro
                    + " OR LOWER(COALESCE(a.documentoIdentidad, '')) LIKE " + parametro + ")");
        }
        if (!condiciones.isEmpty()) {
            desde.append("AND ").append(String.join(" AND ", condiciones)).append(" ");
        }

        String propiedad = "idAlumno";
        String direccion = "ASC";
        if (pageable.getSort().isSorted()) {
            Sort.Order orden = pageable.getSort().iterator().next();
            if (SORT_PERMITIDOS.contains(orden.getProperty())) {
                propiedad = orden.getProperty();
                direccion = orden.isDescending() ? "DESC" : "ASC";
            }
        }

        String orden = "a." + propiedad + " " + direccion;
        if (!"idAlumno".equals(propiedad)) {
            orden += ", a.idAlumno ASC";
        }

        TypedQuery<Alumno> consulta = entityManager.createQuery(
                "SELECT DISTINCT a" + desde + "ORDER BY " + orden, Alumno.class);
        TypedQuery<Long> conteo = entityManager.createQuery("SELECT COUNT(DISTINCT a)" + desde, Long.class);

        aplicarParametros(conteo, conFiltrosAcademicos, accesoActivoId, eliminadoId,
                idNivel, idGrado, idSeccion, idTurno, idGradoSeccion, idAnio, idsVisibles, tokens);
        aplicarParametros(consulta, conFiltrosAcademicos, accesoActivoId, eliminadoId,
                idNivel, idGrado, idSeccion, idTurno, idGradoSeccion, idAnio, idsVisibles, tokens);

        long total = conteo.getSingleResult();
        consulta.setFirstResult((int) pageable.getOffset());
        consulta.setMaxResults(pageable.getPageSize());
        return new PageImpl<>(consulta.getResultList(), pageable, total);
    }

    private void aplicarParametros(
            TypedQuery<?> consulta,
            boolean conFiltrosAcademicos,
            Long accesoActivoId,
            Long eliminadoId,
            Integer idNivel,
            Integer idGrado,
            Integer idSeccion,
            Integer idTurno,
            Integer idGradoSeccion,
            Integer idAnio,
            Collection<Integer> idsVisibles,
            List<String> tokens) {
        consulta.setParameter("eliminadoId", eliminadoId);
        if (conFiltrosAcademicos) {
            consulta.setParameter("accesoActivoId", accesoActivoId);
            if (idNivel != null) consulta.setParameter("idNivel", idNivel);
            if (idGrado != null) consulta.setParameter("idGrado", idGrado);
            if (idSeccion != null) consulta.setParameter("idSeccion", idSeccion);
            if (idTurno != null) consulta.setParameter("idTurno", idTurno);
            if (idGradoSeccion != null) consulta.setParameter("idGradoSeccion", idGradoSeccion);
            if (idAnio != null) consulta.setParameter("idAnio", idAnio);
        }
        if (idsVisibles != null) consulta.setParameter("visibles", idsVisibles);
        for (int i = 0; i < tokens.size(); i++) {
            consulta.setParameter("t" + i, patronLike(tokens.get(i)));
        }
    }

    private static String patronLike(String token) {
        String escapado = token
                .replace(LIKE_ESCAPE, LIKE_ESCAPE + LIKE_ESCAPE)
                .replace("%", LIKE_ESCAPE + "%")
                .replace("_", LIKE_ESCAPE + "_");
        return "%" + escapado + "%";
    }
}
