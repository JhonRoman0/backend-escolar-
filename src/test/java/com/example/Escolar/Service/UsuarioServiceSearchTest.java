package com.example.Escolar.Service;

import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Service.VigenciaService.EstadoVigencia;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceSearchTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private VigenciaService vigenciaService;

    @InjectMocks
    private UsuarioService service;

    private final PageRequest pagina = PageRequest.of(0, 10);

    @Test
    void normalizaMultiplesTerminosYConservaElTotalDelRepositorio() {
        when(usuarioRepository.buscar(
                eq(AccesoConstants.ELIMINADO), isNull(), isNull(), eq(List.of("lucia", "brav")),
                isNull(), isNull(), isNull(), any(LocalDate.class), eq(pagina)))
                .thenReturn(new PageImpl<>(List.of(), pagina, 7));

        Page<?> resultado = service.getAll(pagina, "  LUCÍA   Brav  ", null, null);

        assertEquals(7, resultado.getTotalElements());
        verify(usuarioRepository).buscar(
                eq(AccesoConstants.ELIMINADO), isNull(), isNull(), eq(List.of("lucia", "brav")),
                isNull(), isNull(), isNull(), any(LocalDate.class), eq(pagina));
    }

    @Test
    void reenviaFiltrosYBusquedaVacia() {
        when(usuarioRepository.buscar(
                eq(AccesoConstants.ELIMINADO), isNull(), isNull(), eq(List.of("juan")),
                eq(2), eq(3L), isNull(), any(LocalDate.class), eq(pagina)))
                .thenReturn(new PageImpl<>(List.of(), pagina, 4));
        when(usuarioRepository.buscar(
                eq(AccesoConstants.ELIMINADO), isNull(), isNull(), eq(List.of()),
                isNull(), isNull(), isNull(), any(LocalDate.class), eq(pagina)))
                .thenReturn(new PageImpl<>(List.of(), pagina, 0));

        Page<?> resultado = service.getAll(pagina, "JUAN", 2, 3L);

        assertEquals(4, resultado.getTotalElements());
        assertEquals(0, service.getAll(pagina, "   ", null, null).getTotalElements());
        verify(usuarioRepository).buscar(
                eq(AccesoConstants.ELIMINADO), isNull(), isNull(), eq(List.of()),
                isNull(), isNull(), isNull(), any(LocalDate.class), eq(pagina));
    }

    @Test
    void elReporteUsaBusquedaVaciaYConservaRangoDeFechas() {
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fin = LocalDate.of(2026, 12, 31);
        when(usuarioRepository.buscar(
                eq(AccesoConstants.ELIMINADO), eq(inicio), eq(fin), eq(List.of()),
                isNull(), isNull(), isNull(), any(LocalDate.class), eq(Pageable.unpaged())))
                .thenReturn(new PageImpl<Usuario>(List.of()));

        assertEquals(List.of(), service.reporte(inicio, fin, null, null));
        verify(usuarioRepository).buscar(
                eq(AccesoConstants.ELIMINADO), eq(inicio), eq(fin), eq(List.of()),
                isNull(), isNull(), isNull(), any(LocalDate.class), eq(Pageable.unpaged()));
    }

    @Test
    void reenviaElFiltroDeVigenciaAlListado() {
        when(usuarioRepository.buscar(
                eq(AccesoConstants.ELIMINADO), isNull(), isNull(), eq(List.of("ana")),
                isNull(), isNull(), eq(EstadoVigencia.VIGENTE), any(LocalDate.class), eq(pagina)))
                .thenReturn(new PageImpl<>(List.of(), pagina, 2));

        Page<?> resultado = service.getAll(pagina, "ANA", null, null, EstadoVigencia.VIGENTE);

        assertEquals(2, resultado.getTotalElements());
        verify(usuarioRepository).buscar(
                eq(AccesoConstants.ELIMINADO), isNull(), isNull(), eq(List.of("ana")),
                isNull(), isNull(), eq(EstadoVigencia.VIGENTE), any(LocalDate.class), eq(pagina));
    }

    @Test
    void elReporteReenviaBusquedaYVigencia() {
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fin = LocalDate.of(2026, 12, 31);
        when(usuarioRepository.buscar(
                eq(AccesoConstants.ELIMINADO), eq(inicio), eq(fin), eq(List.of("lucia")),
                eq(2), eq(3L), eq(EstadoVigencia.VENCIDA), any(LocalDate.class), eq(Pageable.unpaged())))
                .thenReturn(new PageImpl<Usuario>(List.of()));

        assertEquals(List.of(), service.reporte(inicio, fin, 2, 3L, "lucia", EstadoVigencia.VENCIDA));
        verify(usuarioRepository).buscar(
                eq(AccesoConstants.ELIMINADO), eq(inicio), eq(fin), eq(List.of("lucia")),
                eq(2), eq(3L), eq(EstadoVigencia.VENCIDA), any(LocalDate.class), eq(Pageable.unpaged()));
    }
}
