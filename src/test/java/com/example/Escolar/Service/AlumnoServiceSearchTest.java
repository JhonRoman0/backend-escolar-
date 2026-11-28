package com.example.Escolar.Service;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Alumno;
import com.example.Escolar.Model.Apoderado;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AlumnoApoderadoRepository;
import com.example.Escolar.Repository.AlumnoRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlumnoServiceSearchTest {
    @Mock private AlumnoRepository alumnoRepository;
    @Mock private AlumnoApoderadoRepository alumnoApoderadoRepository;
    @Mock private MatriculaRepository matriculaRepository;
    @Mock private CloudinaryService cloudinaryService;
    @Mock private AccesoContextoService accesoContextoService;
    @Mock private AccesoRepository accesoRepository;
    @InjectMocks private AlumnoService service;

    private final PageRequest pagina = PageRequest.of(0, 10);
    private final Integer idRolActivo = 1;
    private Acceso activo;

    @BeforeEach
    void preparar() {
        activo = new Acceso();
        activo.setIdAcceso(AccesoConstants.ACTIVO);
        lenient().when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(activo));
        lenient().when(matriculaRepository.findActivasPorAlumnoIds(anyList(), any())).thenReturn(List.of());
        lenient().when(accesoContextoService.tienePermiso(eq("APODERADOS"), any(), anyInt())).thenReturn(false);
    }

    private Alumno alumno(int id, String nombre, String apellido, String codigo, String documento) {
        Alumno alumno = new Alumno();
        alumno.setIdAlumno(id);
        alumno.setNombre(nombre);
        alumno.setApellidoPat(apellido);
        alumno.setCodigo(codigo);
        alumno.setDocumentoIdentidad(documento);
        alumno.setAcceso(activo);
        return alumno;
    }

    private List<Alumno> alumnos(int cantidad) {
        return IntStream.rangeClosed(1, cantidad)
                .mapToObj(i -> alumno(i, "Ana", "Pérez", "00" + i, "7123456" + i))
                .toList();
    }

    @Test
    void gestionNormalizaMultiplesTerminosYConservaElTotalDelRepositorio() {
        when(accesoContextoService.alcance("ALUMNOS", 50, idRolActivo)).thenReturn(AccesoContextoService.Alcance.TODO);
        when(alumnoRepository.buscar(
                eq(List.of("ana", "perez")), eq(AccesoConstants.ACTIVO), eq(AccesoConstants.ELIMINADO),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(pagina)))
                .thenReturn(new PageImpl<>(alumnos(7), pagina, 7));

        Page<?> resultado = service.getAll(pagina, 50, idRolActivo, null, null, null, null, null, null, "  ANA   Pérez  ");

        assertEquals(7, resultado.getTotalElements());
        assertEquals(7, resultado.getContent().size());
        verify(alumnoRepository).buscar(
                eq(List.of("ana", "perez")), eq(AccesoConstants.ACTIVO), eq(AccesoConstants.ELIMINADO),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(pagina));
    }

    @Test
    void apoderadoSinVinculosNoConsultaNIExpansionaLaBusqueda() {
        when(accesoContextoService.alcance("ALUMNOS", 50, idRolActivo)).thenReturn(AccesoContextoService.Alcance.APODERADO);
        when(accesoContextoService.apoderadoDeUsuario(50)).thenReturn(Optional.of(new Apoderado()));
        when(alumnoApoderadoRepository.findByApoderado(any(Apoderado.class))).thenReturn(List.of());

        Page<?> resultado = service.getAll(pagina, 50, idRolActivo, null, null, null, null, null, null, "ana");

        assertEquals(0, resultado.getTotalElements());
        assertTrue(resultado.getContent().isEmpty());
        verifyNoInteractions(alumnoRepository);
    }

    @Test
    void combinaBusquedaConLosFiltrosAcademicos() {
        when(accesoContextoService.alcance("ALUMNOS", 50, idRolActivo)).thenReturn(AccesoContextoService.Alcance.TODO);
        when(alumnoRepository.buscar(
                eq(List.of("juan")), eq(AccesoConstants.ACTIVO), eq(AccesoConstants.ELIMINADO),
                eq(2), eq(3), eq(62), eq(2), eq(602), eq(6), isNull(), eq(pagina)))
                .thenReturn(new PageImpl<>(alumnos(4), pagina, 4));

        Page<?> resultado = service.getAll(pagina, 50, idRolActivo, 2, 3, 62, 2, 602, 6, "JUAN");

        assertEquals(4, resultado.getTotalElements());
        assertEquals(4, resultado.getContent().size());
        verify(alumnoRepository).buscar(
                eq(List.of("juan")), eq(AccesoConstants.ACTIVO), eq(AccesoConstants.ELIMINADO),
                eq(2), eq(3), eq(62), eq(2), eq(602), eq(6), isNull(), eq(pagina));
    }
}
