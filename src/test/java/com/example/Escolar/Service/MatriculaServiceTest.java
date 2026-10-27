package com.example.Escolar.Service;

import com.example.Escolar.Dto.MatriculaResponse;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Alumno;
import com.example.Escolar.Model.AlumnoApoderado;
import com.example.Escolar.Model.Apoderado;
import com.example.Escolar.Model.Matricula;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AlumnoApoderadoRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.HistorialMatriculaRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import com.example.Escolar.Repository.PagoMatriculaRepository;
import com.example.Escolar.Service.AccesoContextoService.Alcance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatriculaServiceTest {
    @Mock private MatriculaRepository matriculaRepository;
    @Mock private HistorialMatriculaRepository historialMatriculaRepository;
    @Mock private AlumnoApoderadoRepository alumnoApoderadoRepository;
    @Mock private GradoSeccionRepository gradoSeccionRepository;
    @Mock private PagoMatriculaRepository pagoMatriculaRepository;
    @Mock private AccesoRepository accesoRepository;
    @Mock private AccesoContextoService accesoContextoService;
    @Spy @InjectMocks private MatriculaService service;

    private final List<String> roles = List.of("Administrador");
    private final Acceso activo = new Acceso(AccesoConstants.ACTIVO, "Activo");
    private final Acceso inactivo = new Acceso(AccesoConstants.INACTIVO, "Inactivo");
    private final Acceso eliminado = new Acceso(AccesoConstants.ELIMINADO, "Eliminado");

    @BeforeEach
    void prepararMapeo() {
        // Estas pruebas aislan seleccion, alcance y paginacion del mapeo de pagos/historial.
        lenient().doAnswer(invocation -> {
            Matricula matricula = invocation.getArgument(0);
            MatriculaResponse response = new MatriculaResponse();
            response.setIdMatricula(matricula.getIdMatricula());
            response.setAccesoId(matricula.getAcceso().getIdAcceso());
            return response;
        }).when(service).toResponse(any(Matricula.class));
    }

    @Test
    void sinEstadoConservaActivasEInactivasYExcluyeEliminadas() {
        var pagina = PageRequest.of(0, 10);
        when(accesoContextoService.alcance("MATRICULAS", 7, roles)).thenReturn(Alcance.TODO);
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        when(matriculaRepository.findByAccesoNot(eliminado, pagina)).thenReturn(
                new PageImpl<>(List.of(matricula(1, activo), matricula(2, inactivo)), pagina, 2));

        var resultado = service.getAll(pagina, 7, roles);

        assertEquals(List.of(1L, 3L), resultado.map(MatriculaResponse::getAccesoId).getContent());
        verify(matriculaRepository).findByAccesoNot(eliminado, pagina);
        verify(matriculaRepository, never()).findByAcceso(any(), any());
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 3})
    void estadoSeConsultaAntesDePaginarYConservaTotales(Long idAcceso) {
        var pagina = PageRequest.of(1, 2);
        var acceso = idAcceso.equals(1L) ? activo : inactivo;
        when(accesoContextoService.alcance("MATRICULAS", 7, roles)).thenReturn(Alcance.TODO);
        when(accesoRepository.findById(idAcceso)).thenReturn(Optional.of(acceso));
        when(matriculaRepository.findByAcceso(acceso, pagina)).thenReturn(
                new PageImpl<>(List.of(matricula(4, acceso), matricula(5, acceso)), pagina, 7));

        var resultado = listar(pagina, idAcceso);

        assertEquals(7, resultado.getTotalElements());
        assertEquals(4, resultado.getTotalPages());
        assertEquals(1, resultado.getNumber());
        assertTrue(resultado.stream().allMatch(m -> m.getAccesoId().equals(idAcceso)));
        verify(matriculaRepository).findByAcceso(acceso, pagina);
        verify(matriculaRepository, never()).findByAccesoNot(any(), any());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 2, -1, 99})
    void rechazaEstadosNoPermitidosSinConsultarDatos(Long idAcceso) {
        assertThrows(IllegalArgumentException.class, () -> listar(PageRequest.of(0, 10), idAcceso));
        verifyNoInteractions(matriculaRepository, accesoRepository, accesoContextoService);
    }

    @Test
    void combinaEstadoConFiltrosAcademicos() {
        var pagina = PageRequest.of(0, 10);
        when(accesoContextoService.alcance("MATRICULAS", 7, roles)).thenReturn(Alcance.TODO);
        when(matriculaRepository.findFiltradas(2L, 4, 5, 6, 8, 9, 2027, 3L, pagina))
                .thenReturn(new PageImpl<>(List.of(matricula(1, inactivo)), pagina, 1));

        var resultado = service.getAll(pagina, 7, roles, 4, 5, 6, 8, 9, 2027, 3L);

        assertEquals(3L, resultado.getContent().get(0).getAccesoId());
        verify(matriculaRepository).findFiltradas(2L, 4, 5, 6, 8, 9, 2027, 3L, pagina);
    }

    @Test
    void docenteSoloPaginaMatriculasDeSuAlcance() {
        var primera = matricula(1, inactivo);
        var segunda = matricula(2, inactivo);
        var tercera = matricula(3, inactivo);
        when(accesoContextoService.alcance("MATRICULAS", 7, roles)).thenReturn(Alcance.DOCENTE);
        when(accesoContextoService.gradoSeccionIdsDeDocente(7)).thenReturn(Set.of(12));
        when(accesoContextoService.matriculasDeGradoSecciones(Set.of(12)))
                .thenReturn(List.of(primera, segunda, tercera));
        when(accesoRepository.findById(3L)).thenReturn(Optional.of(inactivo));
        when(matriculaRepository.findByAcceso(inactivo))
                .thenReturn(List.of(matricula(99, inactivo), primera, segunda, tercera));

        var resultado = listar(PageRequest.of(1, 2), 3L);

        assertEquals(3, resultado.getTotalElements());
        assertEquals(2, resultado.getTotalPages());
        assertEquals(List.of(3), resultado.map(MatriculaResponse::getIdMatricula).getContent());
    }

    @Test
    void filtrosAcademicosRespetanAlcanceAntesDePaginar() {
        var permitida = matricula(2, activo);
        when(accesoContextoService.alcance("MATRICULAS", 7, roles)).thenReturn(Alcance.DOCENTE);
        when(accesoContextoService.gradoSeccionIdsDeDocente(7)).thenReturn(Set.of(12));
        when(accesoContextoService.matriculasDeGradoSecciones(Set.of(12))).thenReturn(List.of(permitida));
        when(matriculaRepository.findFiltradasList(2L, null, null, null, null, null, 2027, 1L))
                .thenReturn(List.of(matricula(99, activo), permitida));

        var resultado = service.getAll(PageRequest.of(0, 1), 7, roles,
                null, null, null, null, null, 2027, 1L);

        assertEquals(1, resultado.getTotalElements());
        assertEquals(List.of(2), resultado.map(MatriculaResponse::getIdMatricula).getContent());
    }

    @Test
    void apoderadoSoloVeMatriculasDeSusHijos() {
        var apoderado = new Apoderado();
        var alumno = new Alumno();
        alumno.setIdAlumno(15);
        var vinculo = new AlumnoApoderado();
        vinculo.setAlumno(alumno);
        var permitida = matricula(2, inactivo);
        when(accesoContextoService.alcance("MATRICULAS", 7, roles)).thenReturn(Alcance.APODERADO);
        when(accesoContextoService.apoderadoDeUsuario(7)).thenReturn(Optional.of(apoderado));
        when(alumnoApoderadoRepository.findByApoderado(apoderado)).thenReturn(List.of(vinculo));
        when(accesoRepository.findById(2L)).thenReturn(Optional.of(eliminado));
        when(matriculaRepository.findByAlumnoApoderadoAlumnoIdAlumnoAndAccesoNot(15, eliminado))
                .thenReturn(List.of(permitida));
        when(accesoRepository.findById(3L)).thenReturn(Optional.of(inactivo));
        when(matriculaRepository.findByAcceso(inactivo)).thenReturn(List.of(matricula(99, inactivo), permitida));

        var resultado = listar(PageRequest.of(0, 10), 3L);

        assertEquals(1, resultado.getTotalElements());
        assertEquals(List.of(2), resultado.map(MatriculaResponse::getIdMatricula).getContent());
    }

    private Page<MatriculaResponse> listar(PageRequest pagina, Long idAcceso) {
        return service.getAll(pagina, 7, roles, null, null, null, null, null, null, idAcceso);
    }

    private Matricula matricula(int id, Acceso acceso) {
        var matricula = new Matricula();
        matricula.setIdMatricula(id);
        matricula.setAcceso(acceso);
        return matricula;
    }
}
