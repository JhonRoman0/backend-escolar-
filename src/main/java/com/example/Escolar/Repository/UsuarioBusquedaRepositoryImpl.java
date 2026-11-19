package com.example.Escolar.Repository;

import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Util.TextoBusqueda;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class UsuarioBusquedaRepositoryImpl implements UsuarioBusquedaRepository {

    private static final Set<String> SORT_PERMITIDOS = Set.of(
            "idUsuario", "nombre", "apellidoPat", "apellidoMat", "codigo",
            "documentoIdentidad", "gmail", "fechaCreacion");

    private final EntityManager entityManager;

    public UsuarioBusquedaRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<Usuario> buscar(
            Long eliminadoId,
            LocalDate inicio,
            LocalDate fin,
            List<String> tokens,
            Integer idRol,
            Long idAcceso,
            Pageable pageable) {
        StringBuilder desde = new StringBuilder(" FROM Usuario u WHERE u.acceso.idAcceso <> :eliminadoId ");

        List<String> condiciones = new ArrayList<>();
        if (inicio != null) condiciones.add("u.fechaCreacion >= :inicio");
        if (fin != null) condiciones.add("u.fechaCreacion <= :fin");
        if (idRol != null) condiciones.add("EXISTS (SELECT 1 FROM UsuarioRol ur "
                + "WHERE ur.usuario.idUsuario = u.idUsuario "
                + "AND ur.rol.idRol = :idRol "
                + "AND ur.rol.acceso.idAcceso <> :eliminadoId)");
        if (idAcceso != null) condiciones.add("u.acceso.idAcceso = :idAcceso");

        String escapeLike = TextoBusqueda.ESCAPE_CLAUSE;
        for (int i = 0; i < tokens.size(); i++) {
            String parametro = ":t" + i + escapeLike;
            condiciones.add("(LOWER(u.nombre) LIKE " + parametro
                    + " OR LOWER(u.apellidoPat) LIKE " + parametro
                    + " OR LOWER(u.apellidoMat) LIKE " + parametro
                    + " OR LOWER(u.codigo) LIKE " + parametro
                    + " OR LOWER(COALESCE(u.documentoIdentidad, '')) LIKE " + parametro
                    + " OR LOWER(COALESCE(u.gmail, '')) LIKE " + parametro + ")");
        }
        if (!condiciones.isEmpty()) {
            desde.append("AND ").append(String.join(" AND ", condiciones)).append(" ");
        }

        String propiedad = "idUsuario";
        String direccion = "ASC";
        if (pageable.getSort().isSorted()) {
            Sort.Order orden = pageable.getSort().iterator().next();
            if (SORT_PERMITIDOS.contains(orden.getProperty())) {
                propiedad = orden.getProperty();
                direccion = orden.isDescending() ? "DESC" : "ASC";
            }
        }

        String orden = "u." + propiedad + " " + direccion;
        if (!"idUsuario".equals(propiedad)) {
            orden += ", u.idUsuario ASC";
        }

        TypedQuery<Usuario> consulta = entityManager.createQuery(
                "SELECT DISTINCT u" + desde + "ORDER BY " + orden, Usuario.class);
        TypedQuery<Long> conteo = entityManager.createQuery("SELECT COUNT(DISTINCT u)" + desde, Long.class);

        aplicarParametros(conteo, eliminadoId, inicio, fin, tokens, idRol, idAcceso);
        aplicarParametros(consulta, eliminadoId, inicio, fin, tokens, idRol, idAcceso);

        long total = conteo.getSingleResult();
        if (pageable.isPaged()) {
            consulta.setFirstResult((int) pageable.getOffset());
            consulta.setMaxResults(pageable.getPageSize());
        }
        return new PageImpl<>(consulta.getResultList(), pageable, total);
    }

    private void aplicarParametros(
            TypedQuery<?> consulta,
            Long eliminadoId,
            LocalDate inicio,
            LocalDate fin,
            List<String> tokens,
            Integer idRol,
            Long idAcceso) {
        consulta.setParameter("eliminadoId", eliminadoId);
        if (inicio != null) consulta.setParameter("inicio", inicio);
        if (fin != null) consulta.setParameter("fin", fin);
        if (idRol != null) consulta.setParameter("idRol", idRol);
        if (idAcceso != null) consulta.setParameter("idAcceso", idAcceso);
        for (int i = 0; i < tokens.size(); i++) {
            consulta.setParameter("t" + i, TextoBusqueda.patronLike(tokens.get(i)));
        }
    }
}
