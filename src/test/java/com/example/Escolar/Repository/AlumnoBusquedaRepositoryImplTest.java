package com.example.Escolar.Repository;

import com.example.Escolar.Model.Alumno;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AlumnoBusquedaRepositoryImplTest {

    @Test
    @SuppressWarnings("unchecked")
    void construyeUnLikePorTerminoConVisibilidadYPaginacionEnSql() {
        EntityManager entityManager = mock(EntityManager.class);
        TypedQuery<Alumno> consulta = mock(TypedQuery.class);
        TypedQuery<Long> conteo = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Alumno.class))).thenReturn(consulta);
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(conteo);
        when(consulta.setParameter(anyString(), any())).thenReturn(consulta);
        when(conteo.setParameter(anyString(), any())).thenReturn(conteo);
        when(conteo.getSingleResult()).thenReturn(3L);
        when(consulta.getResultList()).thenReturn(List.of(new Alumno()));

        Pageable pageable = PageRequest.of(0, 2, Sort.by("idAlumno"));
        Page<Alumno> resultado = new AlumnoBusquedaRepositoryImpl(entityManager).buscar(
                List.of("ana", "perez"), 1L, 2L,
                null, null, null, null, null, null, Set.of(1, 2), pageable);

        assertEquals(3, resultado.getTotalElements());
        assertEquals(1, resultado.getContent().size());

        ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
        verify(entityManager, times(2)).createQuery(jpql.capture(), any(Class.class));
        String seleccion = jpql.getAllValues().stream()
                .filter(sql -> sql.startsWith("SELECT DISTINCT"))
                .findFirst()
                .orElseThrow();
        assertTrue(seleccion.contains("LOWER(a.nombre) LIKE :t0 ESCAPE '!'"));
        assertTrue(seleccion.contains("LOWER(a.nombre) LIKE :t1 ESCAPE '!'"));
        assertTrue(seleccion.contains("a.idAlumno IN :visibles"));
        assertTrue(seleccion.contains("ORDER BY a.idAlumno ASC"));
        assertFalse(seleccion.contains("JOIN Matricula"));

        verify(conteo).setParameter("eliminadoId", 2L);
        verify(conteo).setParameter("visibles", Set.of(1, 2));
        verify(conteo).setParameter("t0", "%ana%");
        verify(conteo).setParameter("t1", "%perez%");
        verify(consulta).setFirstResult(0);
        verify(consulta).setMaxResults(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void agregaJoinsAcademicosYCuentaCuandoHayFiltros() {
        EntityManager entityManager = mock(EntityManager.class);
        TypedQuery<Alumno> consulta = mock(TypedQuery.class);
        TypedQuery<Long> conteo = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Alumno.class))).thenReturn(consulta);
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(conteo);
        when(consulta.setParameter(anyString(), any())).thenReturn(consulta);
        when(conteo.setParameter(anyString(), any())).thenReturn(conteo);
        when(conteo.getSingleResult()).thenReturn(4L);

        Pageable pageable = PageRequest.of(1, 2);
        new AlumnoBusquedaRepositoryImpl(entityManager).buscar(
                List.of("juan"), 1L, 2L,
                null, null, null, null, null, 6, null, pageable);

        ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
        verify(entityManager, times(2)).createQuery(jpql.capture(), any(Class.class));
        String conteoSql = jpql.getAllValues().stream()
                .filter(sql -> sql.startsWith("SELECT COUNT"))
                .findFirst()
                .orElseThrow();
        assertTrue(conteoSql.contains("JOIN Matricula m"));
        assertTrue(conteoSql.contains("anio.idAnio = :idAnio"));
        assertTrue(conteoSql.contains("LOWER(a.nombre) LIKE :t0"));
        verify(conteo).setParameter("accesoActivoId", 1L);
        verify(conteo).setParameter("idAnio", 6);
        verify(conteo).setParameter("t0", "%juan%");
        verify(consulta).setFirstResult(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void escapaComodinesYCaracterDeEscapeEnLosParametrosLike() {
        EntityManager entityManager = mock(EntityManager.class);
        TypedQuery<Alumno> consulta = mock(TypedQuery.class);
        TypedQuery<Long> conteo = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Alumno.class))).thenReturn(consulta);
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(conteo);
        when(consulta.setParameter(anyString(), any())).thenReturn(consulta);
        when(conteo.setParameter(anyString(), any())).thenReturn(conteo);
        when(conteo.getSingleResult()).thenReturn(1L);
        when(consulta.getResultList()).thenReturn(List.of(new Alumno()));

        Pageable pageable = PageRequest.of(0, 10);
        new AlumnoBusquedaRepositoryImpl(entityManager).buscar(
                List.of("50%a_b!"), 1L, 2L,
                null, null, null, null, null, null, null, pageable);

        ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
        verify(entityManager, times(2)).createQuery(jpql.capture(), any(Class.class));
        String seleccion = jpql.getAllValues().stream()
                .filter(sql -> sql.startsWith("SELECT DISTINCT"))
                .findFirst()
                .orElseThrow();
        assertTrue(seleccion.contains("LOWER(a.nombre) LIKE :t0 ESCAPE '!'"));

        verify(conteo).setParameter("t0", "%50!%a!_b!!%");
        verify(consulta).setParameter("t0", "%50!%a!_b!!%");
    }

    @Test
    @SuppressWarnings("unchecked")
    void agregaIdComoDesempateCuandoElOrdenNoEsPorId() {
        EntityManager entityManager = mock(EntityManager.class);
        TypedQuery<Alumno> consulta = mock(TypedQuery.class);
        TypedQuery<Long> conteo = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Alumno.class))).thenReturn(consulta);
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(conteo);
        when(consulta.setParameter(anyString(), any())).thenReturn(consulta);
        when(conteo.setParameter(anyString(), any())).thenReturn(conteo);
        when(conteo.getSingleResult()).thenReturn(1L);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "nombre"));
        new AlumnoBusquedaRepositoryImpl(entityManager).buscar(
                List.of("ana"), 1L, 2L,
                null, null, null, null, null, null, null, pageable);

        ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
        verify(entityManager, times(2)).createQuery(jpql.capture(), any(Class.class));
        String seleccion = jpql.getAllValues().stream()
                .filter(sql -> sql.startsWith("SELECT DISTINCT"))
                .findFirst()
                .orElseThrow();
        assertTrue(seleccion.contains("ORDER BY a.nombre DESC, a.idAlumno ASC"));
    }
}
