package com.example.Escolar.Service;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Alumno;
import com.example.Escolar.Model.AlumnoApoderado;
import com.example.Escolar.Model.Asignacion;
import com.example.Escolar.Model.Apoderado;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.Matricula;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    private void concede(String rol, String permiso, String... acciones) {
        Set<String> permitidas = Set.of(acciones);
        when(concesionesService.accionesPorPermiso(USUARIO)).thenReturn(Map.of(permiso, permitidas));
        when(concesionesService.accionesPorPermisoYRol(USUARIO))
                .thenReturn(Map.of(rol, Map.of(permiso, permitidas)));
    }

    private void esApoderadoConHijos() {
        Apoderado apoderado = new Apoderado();
        Acceso estado = new Acceso();
        estado.setIdAcceso(AccesoConstants.ACTIVO);
        apoderado.setAcceso(estado);
        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(estado));
        when(apoderadoRepository.findByUsuarioIdUsuarioAndAccesoNot(eq(USUARIO), any()))
                .thenReturn(Optional.of(apoderado));

        Alumno alumno = new Alumno();
        alumno.setIdAlumno(7);
        AlumnoApoderado vinculo = new AlumnoApoderado();
        vinculo.setAlumno(alumno);
        vinculo.setActivo(true);
        Alumno otro = new Alumno();
        otro.setIdAlumno(8);
        AlumnoApoderado vinculoInactivo = new AlumnoApoderado();
        vinculoInactivo.setAlumno(otro);
        vinculoInactivo.setActivo(false);
        Matricula matricula = new Matricula();
        matricula.setAcceso(estado);
        when(alumnoApoderadoRepository.findByApoderado(apoderado))
                .thenReturn(List.of(vinculo, vinculoInactivo));
        when(matriculaRepository.findActivasPorAlumnoIds(List.of(7), estado))
                .thenReturn(List.of(matricula));
    }

    private void esDocente() {
        esDocente(AccesoConstants.ACTIVO);
    }

    private void esDocente(Long acceso) {
        Acceso estado = new Acceso();
        estado.setIdAcceso(acceso);
        Docente docente = new Docente();
        docente.setAcceso(estado);
        docente.setIdDocente(11);
        when(docenteRepository.findByUsuarioIdUsuario(USUARIO)).thenReturn(Optional.of(docente));
        if (AccesoConstants.ACTIVO.equals(acceso)) {
            when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(estado));
            when(asignacionRepository.findByDocenteIdDocenteAndAcceso(11, estado))
                    .thenReturn(List.of(new com.example.Escolar.Model.Asignacion()));
        }
    }

    @Test
    void elDocenteConSoloLecturaVeLoSuyo() {
        esDocente();
        concede("DOCENTE", "ASIGNACIONES");
        assertEquals(Alcance.DOCENTE, accesoContextoService.alcance("ASIGNACIONES", USUARIO, ROLES));
    }

    @Test
    void gradoSeccionIdsDeDocenteReutilizaAsignacionesActivas() {
        esDocente();
        GradoSeccion gradoSeccion = new GradoSeccion();
        gradoSeccion.setIdGradoSeccion(701);
        Asignacion activa = new Asignacion();
        activa.setGradoSeccion(gradoSeccion);
        when(asignacionRepository.findByDocenteIdDocenteAndAcceso(eq(11), any()))
                .thenReturn(List.of(activa));

        assertEquals(Set.of(701), accesoContextoService.gradoSeccionIdsDeDocente(USUARIO));

        verify(asignacionRepository, times(1)).findByDocenteIdDocenteAndAcceso(eq(11), any());
        verify(asignacionRepository, never()).findByDocenteIdDocenteAndAccesoNot(any(), any());
    }

    @Test
    void rolPersonalizadoDependeDeLaAsignacionVigente() {
        esDocente();
        concede("Coordinacion", "NOTAS", "CREAR");
        assertEquals(Alcance.DOCENTE, accesoContextoService.alcance(
                "NOTAS", AccesoContextoService.Operacion.ESCRITURA, USUARIO, List.of("Coordinacion")));

        when(asignacionRepository.findByDocenteIdDocenteAndAcceso(eq(11), any())).thenReturn(List.of());
        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance(
                "NOTAS", AccesoContextoService.Operacion.ESCRITURA, USUARIO, List.of("Coordinacion")));
    }

    @Test
    void elDocenteConEscrituraNoSeVuelveGlobal() {
        esDocente();
        concede("DOCENTE", "ASIGNACIONES", "ACTUALIZAR", "ELIMINAR");
        assertEquals(Alcance.DOCENTE, accesoContextoService.alcance("ASIGNACIONES", USUARIO, ROLES));
        assertEquals(Alcance.DOCENTE, accesoContextoService.alcance(
                "ASIGNACIONES", AccesoContextoService.Operacion.ESCRITURA, USUARIO, ROLES));
    }

    @Test
    void imprimirNoCuentaComoEscritura() {
        esDocente();
        concede("DOCENTE", "HORARIOS", "IMPRIMIR_EXPORTAR");
        assertEquals(Alcance.DOCENTE, accesoContextoService.alcance("HORARIOS", USUARIO, ROLES));
        assertEquals(Alcance.DOCENTE, accesoContextoService.alcance(
                "HORARIOS", AccesoContextoService.Operacion.REPORTE, USUARIO, ROLES));
        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance(
                "HORARIOS", AccesoContextoService.Operacion.ESCRITURA, USUARIO, ROLES));
    }

    @Test
    void elApoderadoConSoloLecturaVeLoDeSusHijos() {
        esApoderadoConHijos();
        concede("APODERADO", "NOTAS");
        assertEquals(Alcance.APODERADO, accesoContextoService.alcance("NOTAS", USUARIO, List.of("Apoderado")));
    }

    @Test
    void matriculasDeHijosConsultaUnaSolaVezSoloVinculosActivos() {
        esApoderadoConHijos();
        Apoderado apoderado = accesoContextoService.apoderadoDeUsuario(USUARIO).orElseThrow();

        List<Matricula> matriculas = accesoContextoService.matriculasDeHijos(apoderado);

        assertEquals(1, matriculas.size());
        verify(matriculaRepository, times(1)).findActivasPorAlumnoIds(eq(List.of(7)), any());
        verify(matriculaRepository, never()).findByAlumnoApoderadoAlumnoIdAlumnoAndAccesoNot(any(), any());
    }

    @Test
    void siLeQuitaronElRolDocenteYaNoVeSoloLoSuyo() {
        esDocente(AccesoConstants.ELIMINADO);
        concede("PORTERO", "ASIGNACIONES");
        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance("ASIGNACIONES", USUARIO, List.of("Portero")));
    }

    @Test
    void docenteInactivoNoObtieneAlcanceConUnRolPersonalizado() {
        esDocente(AccesoConstants.INACTIVO);
        concede("PORTERO", "ASIGNACIONES");

        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance("ASIGNACIONES", USUARIO, List.of("Portero")));
    }

    @Test
    void docenteSuspendidoSinOtroRolNoVeTodoConSoloLecturaDeNotas() {
        esDocente(AccesoConstants.INACTIVO);
        concede("DOCENTE", "NOTAS");

        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance("NOTAS", USUARIO, List.of("Docente")));
    }

    @Test
    void variosRolesSinVinculoNoOtorganAlcance() {
        esDocente(AccesoConstants.INACTIVO);
        when(concesionesService.accionesPorPermisoYRol(USUARIO)).thenReturn(Map.of(
                "DOCENTE", Map.of("NOTAS", Set.of()),
                "PORTERO", Map.of("NOTAS", Set.of())));

        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance("NOTAS", USUARIO, List.of("Docente", "Portero")));
    }

    @Test
    void docenteInactivoConApoderadoNoUsaLaEscrituraDocenteSobreSusHijos() {
        esDocente(AccesoConstants.INACTIVO);
        esApoderadoConHijos();
        when(concesionesService.accionesPorPermisoYRol(USUARIO)).thenReturn(Map.of(
                "DOCENTE", Map.of("NOTAS", Set.of("CREAR")),
                "APODERADO", Map.of("NOTAS", Set.of())));

        assertEquals(Alcance.APODERADO, accesoContextoService.alcance(
                "NOTAS", USUARIO, List.of("Docente", "Apoderado")));
        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance(
                "NOTAS", AccesoContextoService.Operacion.ESCRITURA, USUARIO,
                List.of("Docente", "Apoderado")));
    }

    @Test
    void escrituraSinVinculoNoConcedeAlcanceGlobal() {
        concede("SECRETARIA", "NOTAS", "CREAR");
        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance(
                "NOTAS", AccesoContextoService.Operacion.ESCRITURA, USUARIO, List.of("Secretaria")));
    }

    @Test
    void rolPersonalizadoSinVinculoNoVeTodo() {
        concede("PORTERO", "ALUMNOS");
        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance("ALUMNOS", USUARIO, List.of("Portero")));
    }

    @Test
    void elAdminSiempreVeTodo() {
        esDocente();
        assertEquals(Alcance.TODO, accesoContextoService.alcance("NOTAS", USUARIO, List.of("ADMIN")));
    }

    @Test
    void autorizarDependeDelPermisoYNoDelNombreDelRol() {
        concede("SECRETARIA", "NOTAS", "AUTORIZAR");
        assertTrue(accesoContextoService.tieneAccion("NOTAS", "AUTORIZAR", USUARIO, List.of("Secretaria")));

        concede("ADMINISTRADOR", "NOTAS", "CREAR");
        assertFalse(accesoContextoService.tieneAccion("NOTAS", "AUTORIZAR", USUARIO, List.of("Administrador")));
    }
}
