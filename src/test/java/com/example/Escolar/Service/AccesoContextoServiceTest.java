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
import com.example.Escolar.Security.UsuarioAutenticado;
import com.example.Escolar.Service.AccesoContextoService.Alcance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccesoContextoServiceTest {

    private static final Integer USUARIO = 50;
    private static final Integer ROL_ACTIVO = 90;
    private static final Integer OTRO_ROL = 91;

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

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(Integer idUsuario, Integer idRolActivo, List<String> roles) {
        UsuarioAutenticado principal = new UsuarioAutenticado(idUsuario, "COD", roles, 1, idRolActivo);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private void concede(String nombreRol, String permiso, String... acciones) {
        Set<String> permitidas = Set.of(acciones);
        when(concesionesService.rolActivoValido(USUARIO, ROL_ACTIVO)).thenReturn(true);
        when(concesionesService.esAdminActivo(USUARIO, ROL_ACTIVO)).thenReturn(false);
        when(concesionesService.accionesPorPermiso(USUARIO, ROL_ACTIVO)).thenReturn(Map.of(permiso, permitidas));
        when(concesionesService.nombreRolActivo(USUARIO, ROL_ACTIVO)).thenReturn(nombreRol);
    }

    private void adminActivo() {
        when(concesionesService.rolActivoValido(USUARIO, ROL_ACTIVO)).thenReturn(true);
        when(concesionesService.esAdminActivo(USUARIO, ROL_ACTIVO)).thenReturn(true);
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
            when(asignacionRepository.findByDocenteIdDocenteAndAcceso(eq(11), any()))
                    .thenReturn(List.of(new com.example.Escolar.Model.Asignacion()));
        }
    }

    @Test
    void elDocenteConSoloLecturaVeLoSuyo() {
        esDocente();
        concede("DOCENTE", "ASIGNACIONES");
        assertEquals(Alcance.DOCENTE, accesoContextoService.alcance("ASIGNACIONES", USUARIO, ROL_ACTIVO));
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
    void rolPersonalizadoNoHeredaAlcanceDocentePorTenerAsignaciones() {
        esDocente();
        concede("Coordinacion", "NOTAS", "CREAR");
        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance(
                "NOTAS", AccesoContextoService.Operacion.ESCRITURA, USUARIO, ROL_ACTIVO));

        when(asignacionRepository.findByDocenteIdDocenteAndAcceso(eq(11), any())).thenReturn(List.of());
        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance(
                "NOTAS", AccesoContextoService.Operacion.ESCRITURA, USUARIO, ROL_ACTIVO));
    }

    @Test
    void elDocenteConEscrituraNoSeVuelveGlobal() {
        esDocente();
        concede("DOCENTE", "ASIGNACIONES", "ACTUALIZAR", "ELIMINAR");
        assertEquals(Alcance.DOCENTE, accesoContextoService.alcance("ASIGNACIONES", USUARIO, ROL_ACTIVO));
        assertEquals(Alcance.DOCENTE, accesoContextoService.alcance(
                "ASIGNACIONES", AccesoContextoService.Operacion.ESCRITURA, USUARIO, ROL_ACTIVO));
    }

    @Test
    void imprimirNoCuentaComoEscritura() {
        esDocente();
        concede("DOCENTE", "HORARIOS", "IMPRIMIR_EXPORTAR");
        assertEquals(Alcance.DOCENTE, accesoContextoService.alcance("HORARIOS", USUARIO, ROL_ACTIVO));
        assertEquals(Alcance.DOCENTE, accesoContextoService.alcance(
                "HORARIOS", AccesoContextoService.Operacion.REPORTE, USUARIO, ROL_ACTIVO));
        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance(
                "HORARIOS", AccesoContextoService.Operacion.ESCRITURA, USUARIO, ROL_ACTIVO));
    }

    @Test
    void elApoderadoConSoloLecturaVeLoDeSusHijos() {
        esApoderadoConHijos();
        concede("Apoderado", "NOTAS");
        assertEquals(Alcance.APODERADO, accesoContextoService.alcance("NOTAS", USUARIO, ROL_ACTIVO));
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
        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance("ASIGNACIONES", USUARIO, ROL_ACTIVO));
    }

    @Test
    void docenteInactivoNoObtieneAlcanceConUnRolPersonalizado() {
        esDocente(AccesoConstants.INACTIVO);
        concede("PORTERO", "ASIGNACIONES");

        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance("ASIGNACIONES", USUARIO, ROL_ACTIVO));
    }

    @Test
    void docenteSuspendidoSinOtroRolNoVeTodoConSoloLecturaDeNotas() {
        esDocente(AccesoConstants.INACTIVO);
        concede("DOCENTE", "NOTAS");
        when(concesionesService.rolActivoValido(USUARIO, ROL_ACTIVO)).thenReturn(false);

        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance("NOTAS", USUARIO, ROL_ACTIVO));
    }

    @Test
    void variosRolesSinVinculoNoOtorganAlcance() {
        esDocente(AccesoConstants.INACTIVO);
        concede("PORTERO", "NOTAS");
        when(concesionesService.accionesPorPermiso(USUARIO, ROL_ACTIVO)).thenReturn(Map.of("NOTAS", Set.of()));
        when(concesionesService.accionesPorPermiso(USUARIO, OTRO_ROL))
                .thenReturn(Map.of("NOTAS", Set.of("CREAR")));

        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance("NOTAS", USUARIO, ROL_ACTIVO));
    }

    @Test
    void permisosDeOtroRolNoSeCombinanEnEscritura() {
        esDocente();
        concede("DOCENTE", "NOTAS");
        when(concesionesService.accionesPorPermiso(USUARIO, ROL_ACTIVO)).thenReturn(Map.of("NOTAS", Set.of()));
        when(concesionesService.accionesPorPermiso(USUARIO, OTRO_ROL))
                .thenReturn(Map.of("NOTAS", Set.of("CREAR")));
        when(concesionesService.nombreRolActivo(USUARIO, OTRO_ROL)).thenReturn("DOCENTE");

        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance(
                "NOTAS", AccesoContextoService.Operacion.ESCRITURA, USUARIO, ROL_ACTIVO));
    }

    @Test
    void docenteYApoderadoNoSeCombinanSegunRolActivo() {
        esDocente();
        esApoderadoConHijos();
        concede("DOCENTE", "NOTAS");
        assertEquals(Alcance.DOCENTE, accesoContextoService.alcance("NOTAS", USUARIO, ROL_ACTIVO));

        concede("Apoderado", "NOTAS");
        assertEquals(Alcance.APODERADO, accesoContextoService.alcance("NOTAS", USUARIO, ROL_ACTIVO));

        concede("Coordinacion", "NOTAS");
        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance("NOTAS", USUARIO, ROL_ACTIVO));
    }

    @Test
    void docenteInactivoConApoderadoNoUsaLaEscrituraDocenteSobreSusHijos() {
        esDocente(AccesoConstants.INACTIVO);
        esApoderadoConHijos();
        concede("Apoderado", "NOTAS");
        when(concesionesService.accionesPorPermiso(USUARIO, OTRO_ROL))
                .thenReturn(Map.of("NOTAS", Set.of("CREAR")));
        when(concesionesService.nombreRolActivo(USUARIO, OTRO_ROL)).thenReturn("DOCENTE");

        assertEquals(Alcance.APODERADO, accesoContextoService.alcance("NOTAS", USUARIO, ROL_ACTIVO));
        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance(
                "NOTAS", AccesoContextoService.Operacion.ESCRITURA, USUARIO, ROL_ACTIVO));
    }

    @Test
    void escrituraSinVinculoNoConcedeAlcanceGlobal() {
        concede("SECRETARIA", "NOTAS", "CREAR");
        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance(
                "NOTAS", AccesoContextoService.Operacion.ESCRITURA, USUARIO, ROL_ACTIVO));
    }

    @Test
    void permisoOperativoSinVinculoConservaSuRegla() {
        concede("SECRETARIA", "USUARIOS", "CREAR");
        assertEquals(Alcance.TODO, accesoContextoService.alcance(
                "USUARIOS", AccesoContextoService.Operacion.ESCRITURA, USUARIO, ROL_ACTIVO));
    }

    @Test
    void rolPersonalizadoSinVinculoNoVeTodo() {
        concede("PORTERO", "ALUMNOS");
        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance("ALUMNOS", USUARIO, ROL_ACTIVO));
    }

    @Test
    void elAdminActivoSiempreVeTodo() {
        adminActivo();
        assertEquals(Alcance.TODO, accesoContextoService.alcance("NOTAS", USUARIO, ROL_ACTIVO));
        assertEquals(Alcance.TODO, accesoContextoService.vinculoEfectivo(USUARIO, ROL_ACTIVO));
        assertTrue(accesoContextoService.tieneAccion("NOTAS", "AUTORIZAR", USUARIO, ROL_ACTIVO));
        assertTrue(accesoContextoService.tienePermiso("NOTAS", USUARIO, ROL_ACTIVO));
    }

    @Test
    void adminActivoConVinculosPersonalesNoLosHereda() {
        esDocente();
        esApoderadoConHijos();
        adminActivo();
        clearInvocations(docenteRepository, apoderadoRepository, alumnoApoderadoRepository,
                asignacionRepository, concesionesService);

        assertEquals(Alcance.TODO, accesoContextoService.vinculoEfectivo(USUARIO, ROL_ACTIVO));

        verify(docenteRepository, never()).findByUsuarioIdUsuario(USUARIO);
        verify(apoderadoRepository, never()).findByUsuarioIdUsuarioAndAccesoNot(eq(USUARIO), any());
        verify(asignacionRepository, never()).findByDocenteIdDocenteAndAcceso(any(), any());
        verify(alumnoApoderadoRepository, never()).findByApoderado(any());
    }

    @Test
    void sinRolActivoNiegaTodo() {
        concede("ADMIN", "NOTAS");
        Integer sinRol = null;

        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance("NOTAS", USUARIO, sinRol));
        assertEquals(Alcance.NINGUNO, accesoContextoService.vinculoEfectivo(USUARIO, sinRol));
        assertFalse(accesoContextoService.tieneAccion("NOTAS", "CREAR", USUARIO, sinRol));
        assertFalse(accesoContextoService.tienePermiso("NOTAS", USUARIO, sinRol));
    }

    @Test
    void rolNoValidadoNiegaTodo() {
        concede("ADMIN", "NOTAS");
        when(concesionesService.rolActivoValido(USUARIO, ROL_ACTIVO)).thenReturn(false);

        assertEquals(Alcance.NINGUNO, accesoContextoService.alcance("NOTAS", USUARIO, ROL_ACTIVO));
        assertEquals(Alcance.NINGUNO, accesoContextoService.vinculoEfectivo(USUARIO, ROL_ACTIVO));
        assertFalse(accesoContextoService.tieneAccion("NOTAS", "CREAR", USUARIO, ROL_ACTIVO));
        assertFalse(accesoContextoService.tienePermiso("NOTAS", USUARIO, ROL_ACTIVO));
    }

    @Test
    void vinculoEfectivoDependeDelRolActivoYNuncaCombinaRelaciones() {
        esDocente();
        esApoderadoConHijos();
        concede("DOCENTE", "NOTAS");
        assertEquals(Alcance.DOCENTE, accesoContextoService.vinculoEfectivo(USUARIO, ROL_ACTIVO));

        concede("Apoderado", "NOTAS");
        assertEquals(Alcance.APODERADO, accesoContextoService.vinculoEfectivo(USUARIO, ROL_ACTIVO));

        concede("Coordinacion", "NOTAS");
        assertEquals(Alcance.NINGUNO, accesoContextoService.vinculoEfectivo(USUARIO, ROL_ACTIVO));
    }

    @Test
    void autorizarDependeDelPermisoYNoDelNombreDelRol() {
        concede("SECRETARIA", "NOTAS", "AUTORIZAR");
        assertTrue(accesoContextoService.tieneAccion("NOTAS", "AUTORIZAR", USUARIO, ROL_ACTIVO));

        concede("ADMINISTRADOR", "NOTAS", "CREAR");
        assertFalse(accesoContextoService.tieneAccion("NOTAS", "AUTORIZAR", USUARIO, ROL_ACTIVO));
    }
}
