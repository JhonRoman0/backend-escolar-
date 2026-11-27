package com.example.Escolar.Service;

import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Alumno;
import com.example.Escolar.Model.AlumnoApoderado;
import com.example.Escolar.Model.Competencia;
import com.example.Escolar.Model.Curso;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.Matricula;
import com.example.Escolar.Model.Nota;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.CompetenciaRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import com.example.Escolar.Repository.NotaRepository;
import com.example.Escolar.Service.AccesoContextoService.Alcance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotaServiceTest {

    private static final Integer USUARIO = 50;
    private static final List<String> ROLES = List.of("Docente");

    @Mock
    private NotaRepository notaRepository;
    @Mock
    private CompetenciaRepository competenciaRepository;
    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private AccesoContextoService accesoContextoService;
    @Mock
    private AccesoRepository accesoRepository;
    @InjectMocks
    private NotaService notaService;

    @BeforeEach
    void setUp() {
        Acceso eliminado = new Acceso();
        eliminado.setIdAcceso(AccesoConstants.ELIMINADO);
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));

        // Matrícula 1 en la sección 5 y matrícula 2 en la sección 6.
        when(matriculaRepository.findByIdMatriculaAndAccesoNot(eq(1), any())).thenReturn(Optional.of(matricula(1, 5)));
        when(matriculaRepository.findByIdMatriculaAndAccesoNot(eq(2), any())).thenReturn(Optional.of(matricula(2, 6)));

        // El docente solo enseña en la sección 5.
        when(accesoContextoService.alcance("NOTAS", USUARIO, ROLES)).thenReturn(Alcance.DOCENTE);
        var asignacion = new com.example.Escolar.Model.Asignacion();
        asignacion.setGradoSeccion(matricula(1, 5).getGradoSeccion());
        Curso curso = new Curso();
        curso.setIdCurso(20);
        asignacion.setCurso(curso);
        when(accesoContextoService.asignacionesActivasDeDocente(USUARIO)).thenReturn(List.of(asignacion));
    }

    private Matricula matricula(int id, int idGradoSeccion) {
        GradoSeccion gradoSeccion = new GradoSeccion();
        gradoSeccion.setIdGradoSeccion(idGradoSeccion);
        Matricula matricula = new Matricula();
        matricula.setIdMatricula(id);
        matricula.setGradoSeccion(gradoSeccion);
        return matricula;
    }

    @Test
    void elDocenteVeLasNotasDeSusSecciones() {
        assertDoesNotThrow(() -> notaService.getPorMatricula(1, USUARIO, ROLES));
    }

    @Test
    void elDocenteNoVeLasNotasDeOtraSeccion() {
        assertThrows(ResourceNotFoundException.class, () -> notaService.getPorMatricula(2, USUARIO, ROLES));
    }

    @Test
    void elReporteRespetaElAlcanceDocente() {
        when(notaRepository.findByFechaRegistroBetweenAndAccesoNot(any(), any(), any()))
                .thenReturn(List.of(nota(1, 5), nota(2, 6)));

        var resultado = notaService.reporte(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                null, null, USUARIO, ROLES);

        assertEquals(1, resultado.size());
        assertEquals("Ana Pérez", resultado.get(0).getAlumno());
    }

    @Test
    void sinVinculoDocenteConAlcanceNingunoNoVeNotasNiReportes() {
        when(accesoContextoService.alcance("NOTAS", USUARIO, ROLES)).thenReturn(Alcance.NINGUNO);
        when(notaRepository.findByFechaRegistroBetweenAndAccesoNot(any(), any(), any()))
                .thenReturn(List.of(nota(1, 5), nota(2, 6)));

        var resultado = notaService.reporte(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                null, null, USUARIO, ROLES);

        assertEquals(0, resultado.size());
        assertThrows(ResourceNotFoundException.class, () -> notaService.getPorMatricula(1, USUARIO, ROLES));
    }

    private Nota nota(int idMatricula, int idGradoSeccion) {
        Alumno alumno = new Alumno();
        alumno.setNombre("Ana");
        alumno.setApellidoPat("Pérez");
        alumno.setCodigo("A" + idMatricula);
        AlumnoApoderado alumnoApoderado = new AlumnoApoderado();
        alumnoApoderado.setAlumno(alumno);
        Matricula matricula = matricula(idMatricula, idGradoSeccion);
        matricula.setAlumnoApoderado(alumnoApoderado);
        Curso curso = new Curso();
        curso.setIdCurso(20);
        curso.setNombre("Matemática");
        Competencia competencia = new Competencia();
        competencia.setNombre("Competencia");
        competencia.setCurso(curso);
        Usuario usuario = new Usuario();
        usuario.setNombre("Doc");
        usuario.setApellidoPat("ente");
        Nota nota = new Nota();
        nota.setMatricula(matricula);
        nota.setCompetencia(competencia);
        nota.setBimestre((byte) 1);
        nota.setCalificacion("15");
        nota.setFechaRegistro(LocalDate.of(2026, 3, 10));
        nota.setUsuarioRegistro(usuario);
        return nota;
    }
}
