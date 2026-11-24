package com.example.Escolar.Repository;

import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Service.VigenciaService.EstadoVigencia;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UsuarioBusquedaRepositoryImplTest {

    @Test
    @SuppressWarnings("unchecked")
    void construyeUnLikePorTerminoConFiltrosYPaginacion() {
        EntityManager entityManager = mock(EntityManager.class);
        TypedQuery<Usuario> consulta = mock(TypedQuery.class);
        TypedQuery<Long> conteo = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Usuario.class))).thenReturn(consulta);
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(conteo);
        when(consulta.setParameter(anyString(), any())).thenReturn(consulta);
        when(conteo.setParameter(anyString(), any())).thenReturn(conteo);
        when(conteo.getSingleResult()).thenReturn(3L);
        when(consulta.getResultList()).thenReturn(List.of(new Usuario()));

        Pageable pageable = PageRequest.of(0, 2, Sort.by("idUsuario"));
        Page<Usuario> resultado = new UsuarioBusquedaRepositoryImpl(entityManager).buscar(
                2L, null, null, List.of("lucia", "brav"), 3, 4L, null, null, pageable);

        assertEquals(3, resultado.getTotalElements());
        assertEquals(1, resultado.getContent().size());

        ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
        verify(entityManager, times(2)).createQuery(jpql.capture(), any(Class.class));
        String seleccion = jpql.getAllValues().stream()
                .filter(sql -> sql.startsWith("SELECT DISTINCT"))
                .findFirst()
                .orElseThrow();
        assertTrue(seleccion.contains("LOWER(u.nombre) LIKE :t0 ESCAPE '!'"));
        assertTrue(seleccion.contains("LOWER(u.nombre) LIKE :t1 ESCAPE '!'"));
        assertTrue(seleccion.contains("EXISTS (SELECT 1 FROM UsuarioRol ur"));
        assertTrue(seleccion.contains("u.acceso.idAcceso = :idAcceso"));
        assertTrue(seleccion.contains("ORDER BY u.idUsuario ASC"));

        verify(conteo).setParameter("eliminadoId", 2L);
        verify(conteo).setParameter("idRol", 3);
        verify(conteo).setParameter("idAcceso", 4L);
        verify(conteo).setParameter("t0", "%lucia%");
        verify(conteo).setParameter("t1", "%brav%");
        verify(consulta).setFirstResult(0);
        verify(consulta).setMaxResults(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void conservaFechasYAgregaIdComoDesempate() {
        EntityManager entityManager = mock(EntityManager.class);
        TypedQuery<Usuario> consulta = mock(TypedQuery.class);
        TypedQuery<Long> conteo = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Usuario.class))).thenReturn(consulta);
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(conteo);
        when(consulta.setParameter(anyString(), any())).thenReturn(consulta);
        when(conteo.setParameter(anyString(), any())).thenReturn(conteo);
        when(conteo.getSingleResult()).thenReturn(1L);

        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fin = LocalDate.of(2026, 12, 31);
        Pageable pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "apellidoPat"));
        new UsuarioBusquedaRepositoryImpl(entityManager).buscar(
                2L, inicio, fin, List.of("lucia"), null, null, null, null, pageable);

        ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
        verify(entityManager, times(2)).createQuery(jpql.capture(), any(Class.class));
        String seleccion = jpql.getAllValues().stream()
                .filter(sql -> sql.startsWith("SELECT DISTINCT"))
                .findFirst()
                .orElseThrow();
        assertTrue(seleccion.contains("u.fechaCreacion >= :inicio"));
        assertTrue(seleccion.contains("u.fechaCreacion <= :fin"));
        assertTrue(seleccion.contains("ORDER BY u.apellidoPat DESC, u.idUsuario ASC"));

        verify(conteo).setParameter("inicio", inicio);
        verify(conteo).setParameter("fin", fin);
    }

    @Test
    @SuppressWarnings("unchecked")
    void noLimitaCuandoPageableNoEstaPaginado() {
        EntityManager entityManager = mock(EntityManager.class);
        TypedQuery<Usuario> consulta = mock(TypedQuery.class);
        TypedQuery<Long> conteo = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Usuario.class))).thenReturn(consulta);
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(conteo);
        when(consulta.setParameter(anyString(), any())).thenReturn(consulta);
        when(conteo.setParameter(anyString(), any())).thenReturn(conteo);
        when(conteo.getSingleResult()).thenReturn(1L);
        when(consulta.getResultList()).thenReturn(List.of(new Usuario()));

        new UsuarioBusquedaRepositoryImpl(entityManager).buscar(
                2L, null, null, List.of(), null, null, null, null, Pageable.unpaged());

        verify(consulta, never()).setFirstResult(anyInt());
        verify(consulta, never()).setMaxResults(anyInt());
    }

    @Test
    @SuppressWarnings("unchecked")
    void agregaElPredicadoDelEstadoDeVigenciaYElParametroHoy() {
        LocalDate hoy = LocalDate.of(2026, 11, 24);

        assertTrue(seleccionPara(EstadoVigencia.NO_INICIADA, hoy)
                .contains("u.fechaInicioVigencia IS NOT NULL AND u.fechaInicioVigencia > :hoy"));
        assertTrue(seleccionPara(EstadoVigencia.INDEFINIDA, hoy)
                .contains("u.fechaInicioVigencia IS NOT NULL AND u.fechaInicioVigencia <= :hoy "
                        + "AND u.fechaFinVigencia IS NULL"));
        assertTrue(seleccionPara(EstadoVigencia.VIGENTE, hoy)
                .contains("u.fechaInicioVigencia IS NOT NULL AND u.fechaInicioVigencia <= :hoy "
                        + "AND u.fechaFinVigencia IS NOT NULL AND u.fechaFinVigencia >= :hoy"));
        assertTrue(seleccionPara(EstadoVigencia.VENCIDA, hoy).contains("u.fechaFinVigencia < :hoy"));
    }

    @SuppressWarnings("unchecked")
    private String seleccionPara(EstadoVigencia estado, LocalDate hoy) {
        EntityManager entityManager = mock(EntityManager.class);
        TypedQuery<Usuario> consulta = mock(TypedQuery.class);
        TypedQuery<Long> conteo = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Usuario.class))).thenReturn(consulta);
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(conteo);
        when(consulta.setParameter(anyString(), any())).thenReturn(consulta);
        when(conteo.setParameter(anyString(), any())).thenReturn(conteo);
        when(conteo.getSingleResult()).thenReturn(0L);
        when(consulta.getResultList()).thenReturn(List.of());

        new UsuarioBusquedaRepositoryImpl(entityManager).buscar(
                2L, null, null, List.of(), null, null, estado, hoy, Pageable.unpaged());

        ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
        verify(entityManager, times(2)).createQuery(jpql.capture(), any(Class.class));
        verify(conteo).setParameter("hoy", hoy);
        verify(consulta).setParameter("hoy", hoy);
        return jpql.getAllValues().stream()
                .filter(sql -> sql.startsWith("SELECT DISTINCT"))
                .findFirst()
                .orElseThrow();
    }
}
