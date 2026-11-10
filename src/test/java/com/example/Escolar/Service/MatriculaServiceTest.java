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
import com.example.Escolar.Model.*;
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
    @Mock BloqueoCalificaciones bloqueo;
    @InjectMocks private MatriculaService service;

    private final List<String> roles = List.of("Administrador");
    private final Acceso activo = new Acceso(AccesoConstants.ACTIVO, "Activo");
    private final Acceso inactivo = new Acceso(AccesoConstants.INACTIVO, "Inactivo");
    private final Acceso eliminado = new Acceso(AccesoConstants.ELIMINADO, "Eliminado");

    @BeforeEach
    void prepararMapeo() {
        lenient().when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
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

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"2026,2026,NUEVO_INGRESO", "2025,2026,CONTINUANTE", "2018,2026,CONTINUANTE", "2026,2024,NUEVO_INGRESO"})
    void clasificaSegunElAnioDeLaMatricula(String primero, String actual, MatriculaResponse.TipoIngreso esperado) {
        var m = matricula(1, activo); m.getGradoSeccion().getAnioEscolar().setAnio(actual);
        var resumen = org.mockito.Mockito.mock(MatriculaRepository.PrimerAnioAlumno.class);
        when(resumen.getIdAlumno()).thenReturn(1); when(resumen.getPrimerAnio()).thenReturn(primero);
        when(matriculaRepository.primerosAniosPorAlumno(List.of(1), AccesoConstants.ELIMINADO)).thenReturn(List.of(resumen));
        assertEquals(esperado, service.toResponse(m).getTipoIngreso());
        verify(matriculaRepository).primerosAniosPorAlumno(List.of(1), AccesoConstants.ELIMINADO);
    }

    @Test
    void detalleDevuelveClasificacion() {
        var m = matricula(1, activo);
        when(matriculaRepository.findByIdMatriculaAndAccesoNot(1, eliminado)).thenReturn(Optional.of(m));
        when(accesoContextoService.alcance("MATRICULAS", 7, roles)).thenReturn(Alcance.TODO);
        assertEquals(MatriculaResponse.TipoIngreso.NUEVO_INGRESO, service.getById(1, 7, roles).getTipoIngreso());
        verify(matriculaRepository).primerosAniosPorAlumno(List.of(1), AccesoConstants.ELIMINADO);
    }

    @Test
    void paginaVaciaNoConsultaAntecedentes() {
        when(accesoContextoService.alcance("MATRICULAS", 7, roles)).thenReturn(Alcance.TODO);
        var pagina = PageRequest.of(0, 10);
        when(matriculaRepository.findByAccesoNot(eliminado, pagina)).thenReturn(Page.empty(pagina));
        assertTrue(listar(pagina, null).isEmpty());
        verify(matriculaRepository, never()).primerosAniosPorAlumno(any(), any());
    }

    @Test
    void paginaConsultaAntecedentesUnaSolaVez() {
        when(accesoContextoService.alcance("MATRICULAS", 7, roles)).thenReturn(Alcance.TODO);
        var pagina = PageRequest.of(0, 10);
        when(matriculaRepository.findByAccesoNot(eliminado, pagina)).thenReturn(new PageImpl<>(List.of(matricula(1, activo), matricula(2, inactivo))));
        var resultado = listar(pagina, null);
        assertTrue(resultado.stream().allMatch(m -> m.getTipoIngreso() == MatriculaResponse.TipoIngreso.NUEVO_INGRESO));
        verify(matriculaRepository).primerosAniosPorAlumno(List.of(1, 2), AccesoConstants.ELIMINADO);
    }
    private Page<MatriculaResponse> listar(PageRequest pagina, Long idAcceso) {
        return service.getAll(pagina, 7, roles, null, null, null, null, null, null, idAcceso);
    }

    private Matricula matricula(int id, Acceso acceso) {
        var matricula = new Matricula();
        matricula.setIdMatricula(id);
        matricula.setAcceso(acceso);
        var alumno = new Alumno(); alumno.setIdAlumno(id); alumno.setNombre("Alumno"); alumno.setApellidoPat("Prueba"); alumno.setApellidoMat("Prueba");
        var usuario = new Usuario(); usuario.setIdUsuario(7); usuario.setNombre("Usuario"); usuario.setApellidoPat("Prueba");
        var apoderado = new Apoderado(); apoderado.setUsuario(usuario);
        var vinculo = new AlumnoApoderado(); vinculo.setAlumno(alumno); vinculo.setApoderado(apoderado);
        var nivel = new Nivel(); nivel.setIdNivel(1); nivel.setNombre("Primaria");
        var grado = new Grado(); grado.setIdGrado(1); grado.setNombre("Primero"); grado.setNivel(nivel);
        var anio = new AnioEscolar(); anio.setIdAnio(1); anio.setAnio("2026");
        var turno = new Turno(); turno.setIdTurno(1); turno.setNombre("Turno");
        var seccion = new GradoSeccion(); seccion.setIdGradoSeccion(12); seccion.setGrado(grado); seccion.setAnioEscolar(anio); seccion.setTurno(turno);
        matricula.setAlumnoApoderado(vinculo); matricula.setUsuario(usuario); matricula.setGradoSeccion(seccion);
        return matricula;
    }
}
