package com.example.Escolar.Service;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Apoderado;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AlumnoApoderadoRepository;
import com.example.Escolar.Repository.ApoderadoRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.DocenteRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import com.example.Escolar.Security.ConcesionesService;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccesoContextoServiceTest {

    private static final Integer USUARIO = 50;
    private static final List<String> ROLES = List.of("Docente");

    @Mock
    private DocenteRepository docenteRepository;
    @Mock
    private ApoderadoRepository apoderadoRepository;
    @Mock
    private AlumnoApoderadoRepository alumnoApoderadoRepository;
    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private AsignacionRepository asignacionRepository;
    @Mock
    private AccesoRepository accesoRepository;
    @Mock
    private ConcesionesService concesionesService;
    @InjectMocks
    private AccesoContextoService accesoContextoService;

    @BeforeEach
    void setUp() {
        Acceso eliminado = new Acceso();
        eliminado.setIdAcceso(AccesoConstants.ELIMINADO);
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        when(docenteRepository.findByUsuarioIdUsuario(any())).thenReturn(Optional.empty());
        when(apoderadoRepository.findByUsuarioIdUsuarioAndAccesoNot(any(), any())).thenReturn(Optional.empty());
    }

    private void concede(String permiso, String... acciones) {
        when(concesionesService.accionesPorPermiso(USUARIO)).thenReturn(Map.of(permiso, Set.of(acciones)));
    }

    private void esDocente() {
        esDocente(AccesoConstants.ACTIVO);
    }

    private void esDocente(Long acceso) {
        Acceso estado = new Acceso();
        estado.setIdAcceso(acceso);
        Docente docente = new Docente();
        docente.setAcceso(estado);
        when(docenteRepository.findByUsuarioIdUsuario(USUARIO)).thenReturn(Optional.of(docente));
    }

    @Test
    void elDocenteConSoloLecturaVeLoSuyo() {
        esDocente();
        concede("ASIGNACIONES");
        assertEquals(Alcance.DOCENTE, accesoContextoService.alcance("ASIGNACIONES", USUARIO, ROLES));
    }

    @Test
    void elDocenteConEscrituraVeTodo() {
        esDocente();
        concede("ASIGNACIONES", "ACTUALIZAR", "ELIMINAR");
        assertEquals(Alcance.TODO, accesoContextoService.alcance("ASIGNACIONES", USUARIO, ROLES));
    }

    @Test
    void imprimirNoCuentaComoEscritura() {
        esDocente();
        concede("HORARIOS", "IMPRIMIR_EXPORTAR");
        assertEquals(Alcance.DOCENTE, accesoContextoService.alcance("HORARIOS", USUARIO, ROLES));
    }

    @Test
    void elApoderadoConSoloLecturaVeLoDeSusHijos() {
        when(apoderadoRepository.findByUsuarioIdUsuarioAndAccesoNot(eq(USUARIO), any())).thenReturn(Optional.of(new Apoderado()));
        concede("NOTAS");
        assertEquals(Alcance.APODERADO, accesoContextoService.alcance("NOTAS", USUARIO, List.of("Apoderado")));
    }

    @Test
    void siLeQuitaronElRolDocenteYaNoVeSoloLoSuyo() {
        esDocente(AccesoConstants.ELIMINADO);
        concede("ASIGNACIONES");
        assertEquals(Alcance.TODO, accesoContextoService.alcance("ASIGNACIONES", USUARIO, List.of("Portero")));
    }

    @Test
    void unRolSinVinculoConSoloLecturaVeTodo() {
        concede("ALUMNOS");
        assertEquals(Alcance.TODO, accesoContextoService.alcance("ALUMNOS", USUARIO, List.of("Portero")));
    }

    @Test
    void elAdminSiempreVeTodo() {
        esDocente();
        assertEquals(Alcance.TODO, accesoContextoService.alcance("NOTAS", USUARIO, List.of("ADMIN")));
    }

    @Test
    void autorizarDependeDelPermisoYNoDelNombreDelRol() {
        concede("NOTAS", "AUTORIZAR");
        assertTrue(accesoContextoService.tieneAccion("NOTAS", "AUTORIZAR", USUARIO, List.of("Secretaria")));

        concede("NOTAS", "CREAR");
        assertFalse(accesoContextoService.tieneAccion("NOTAS", "AUTORIZAR", USUARIO, List.of("Administrador")));
    }
}
