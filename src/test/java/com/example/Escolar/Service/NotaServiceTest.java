package com.example.Escolar.Service;

import com.example.Escolar.Exception.AutorizacionRequeridaException;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.Matricula;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.CompetenciaRepository;
import com.example.Escolar.Repository.DocenteRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import com.example.Escolar.Repository.NotaAutorizacionRepository;
import com.example.Escolar.Repository.NotaRepository;
import com.example.Escolar.Repository.UsuarioRepository;
import com.example.Escolar.Service.AccesoContextoService.Alcance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
    private NotaAutorizacionRepository notaAutorizacionRepository;
    @Mock
    private DocenteRepository docenteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
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
        when(accesoContextoService.gradoSeccionIdsDeDocente(USUARIO)).thenReturn(Set.of(5));
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
    void sinElPermisoDeAutorizarNoGeneraCodigos() {
        when(accesoContextoService.tieneAccion("NOTAS", "AUTORIZAR", USUARIO, ROLES)).thenReturn(false);
        assertThrows(AutorizacionRequeridaException.class, () -> notaService.generarAutorizacion(USUARIO, ROLES, null));
    }
}
