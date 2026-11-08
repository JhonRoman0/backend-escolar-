package com.example.Escolar.Service;

import com.example.Escolar.Dto.ConfiguracionAcademicaResponse;
import com.example.Escolar.Dto.NotaFinalRequest;
import com.example.Escolar.Dto.NotaFinalResponse;
import com.example.Escolar.Exception.AutorizacionRequeridaException;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotaFinalServiceTest {
    @Mock NotaRepository notaRepository;
    @Mock AsignacionRepository asignacionRepository;
    @Mock GradoSeccionRepository gradoSeccionRepository;
    @Mock CursoRepository cursoRepository;
    @Mock CompetenciaRepository competenciaRepository;
    @Mock MatriculaRepository matriculaRepository;
    @Mock EvaluacionRepository evaluacionRepository;
    @Mock NotaEvaluacionRepository notaEvaluacionRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock AccesoRepository accesoRepository;
    @Mock AccesoContextoService accesoContextoService;
    @Mock ConfiguracionAcademicaService configuracionService;
    @Mock NotaService notaService;
    @InjectMocks NotaFinalService service;

    private final Acceso activo = new Acceso(AccesoConstants.ACTIVO, "Activo");
    private final Acceso eliminado = new Acceso(AccesoConstants.ELIMINADO, "Eliminado");
    private final List<String> roles = List.of("Docente");
    private final LocalDate hoy = LocalDate.now();
    private AnioEscolar anio;
    private GradoSeccion gs;
    private Curso matematica;
    private Curso aritmetica;
    private Curso geometria;
    private Competencia c1;
    private Competencia c2;
    private Competencia c3;
    private Matricula ana;
    private ConfiguracionAcademicaResponse config;
    private PeriodoEvaluacion periodo;
    private Docente docenteAritmetica;

    @BeforeEach
    void preparar() {
        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(activo));
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario("LUIS")));
        anio = new AnioEscolar();
        anio.setIdAnio(2);
        anio.setEstado(AnioEscolarService.ESTADO_VIGENTE);
        Nivel nivel = new Nivel();
        nivel.setIdNivel(3);
        Grado grado = new Grado();
        grado.setNivel(nivel);
        grado.setNombre("2do");
        Turno turno = new Turno();
        turno.setNombre("Mañana");
        gs = new GradoSeccion();
        gs.setIdGradoSeccion(30);
        gs.setGrado(grado);
        gs.setTurno(turno);
        gs.setAnioEscolar(anio);
        when(gradoSeccionRepository.findById(30)).thenReturn(Optional.of(gs));

        c1 = competencia(1);
        c2 = competencia(2);
        c3 = competencia(3);
        matematica = curso(100, "MATEMÁTICA", TipoCurso.AREA, null);
        aritmetica = curso(101, "Aritmética", TipoCurso.SUBCURSO, matematica);
        aritmetica.setCompetencias(Set.of(c1, c2));
        geometria = curso(102, "Geometría", TipoCurso.SUBCURSO, matematica);
        geometria.setCompetencias(Set.of(c1, c3));

        docenteAritmetica = new Docente();
        docenteAritmetica.setIdDocente(5);
        Asignacion suya = asignacion(10, aritmetica, docenteAritmetica);
        Asignacion ajena = asignacion(11, geometria, new Docente());
        when(accesoContextoService.docenteDeUsuario(7)).thenReturn(Optional.of(docenteAritmetica));
        when(asignacionRepository.findByDocenteIdDocenteAndAccesoNot(5, eliminado)).thenReturn(List.of(suya));
        when(asignacionRepository.findByAccesoNot(eliminado)).thenReturn(List.of(suya, ajena));

        ana = matricula(1, "ANA");
        when(matriculaRepository.findByGradoSeccionIdGradoSeccionAndAccesoNot(30, eliminado)).thenReturn(List.of(ana));

        config = new ConfiguracionAcademicaResponse();
        config.setEscala(EscalaCalificacion.LITERAL);
        config.setTipoPeriodo(TipoPeriodo.BIMESTRE);
        config.setPeriodos(List.of());
        when(configuracionService.obtener(2, 3)).thenReturn(config);
        periodo = new PeriodoEvaluacion();
        periodo.setNumero((byte) 1);
        abrirPeriodo(true);
        when(configuracionService.periodos(any(), any())).thenReturn(List.of(periodo));
        when(notaRepository.findByMatriculaIdMatriculaInAndCompetenciaIdCompetenciaInAndBimestreAndAccesoNot(
                any(), any(), anyByte(), eq(eliminado))).thenReturn(List.of());
    }

    private void abrirPeriodo(boolean abierto) {
        periodo.setFechaInicio(abierto ? hoy.minusDays(5) : hoy.minusDays(40));
        periodo.setFechaFin(abierto ? hoy.plusDays(5) : hoy.minusDays(1));
    }

    private Usuario usuario(String nombre) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setApellidoPat("DIAZ");
        return u;
    }

    private Competencia competencia(int id) {
        Competencia c = new Competencia();
        c.setIdCompetencia(id);
        c.setCodigo("0" + id);
        c.setNombre("Competencia " + id);
        c.setOrden((byte) id);
        c.setAcceso(activo);
        return c;
    }

    private Curso curso(int id, String nombre, TipoCurso tipo, Curso padre) {
        Curso c = new Curso();
        c.setIdCurso(id);
        c.setNombre(nombre);
        c.setTipo(tipo);
        c.setCursoPadre(padre);
        c.setAcceso(activo);
        return c;
    }

    private Asignacion asignacion(int id, Curso curso, Docente docente) {
        Asignacion a = new Asignacion();
        a.setIdAsignacion(id);
        a.setCurso(curso);
        a.setDocente(docente);
        a.setGradoSeccion(gs);
        a.setAnioEscolar(anio);
        a.setAcceso(activo);
        return a;
    }

    private Matricula matricula(int id, String nombre) {
        Alumno a = new Alumno();
        a.setNombre(nombre);
        a.setApellidoPat("PEREZ");
        a.setApellidoMat("RUIZ");
        AlumnoApoderado vinculo = new AlumnoApoderado();
        vinculo.setAlumno(a);
        Matricula m = new Matricula();
        m.setIdMatricula(id);
        m.setAlumnoApoderado(vinculo);
        m.setGradoSeccion(gs);
        m.setAcceso(activo);
        return m;
    }

    private NotaFinalRequest pedido(int competencia, String calificacion, String conclusion) {
        return new NotaFinalRequest(30, 100, 1, null,
                List.of(new NotaFinalRequest.Item(1, competencia, calificacion, conclusion)));
    }

    private Nota existente(int competencia, String calificacion) {
        Nota n = new Nota();
        n.setMatricula(ana);
        n.setCompetencia(competencia == 1 ? c1 : competencia == 2 ? c2 : c3);
        n.setCalificacion(calificacion);
        n.setUsuarioRegistro(usuario("ROSA"));
        when(notaRepository.findByMatriculaIdMatriculaInAndCompetenciaIdCompetenciaInAndBimestreAndAccesoNot(
                any(), any(), anyByte(), eq(eliminado))).thenReturn(List.of(n));
        return n;
    }

    @Test
    void elDocenteDeUnSubcursoSoloVeSusCompetencias() {
        List<NotaFinalResponse.Curso> cursos = service.cursos(2, 7, roles);

        assertEquals(1, cursos.size());
        assertEquals("MATEMÁTICA", cursos.get(0).area());
        assertEquals(List.of("01", "02"), cursos.get(0).competencias().stream().map(c -> c.codigo()).toList());
    }

    @Test
    void registraUnaNotaNueva() {
        service.registrar(pedido(1, "a", null), 7, roles);

        ArgumentCaptor<Nota> captor = ArgumentCaptor.forClass(Nota.class);
        verify(notaRepository).save(captor.capture());
        assertEquals("A", captor.getValue().getCalificacion());
        assertEquals(1, captor.getValue().getBimestre());
    }

    @Test
    void noCalificaUnaCompetenciaDeOtroSubcurso() {
        var ex = assertThrows(IllegalArgumentException.class, () -> service.registrar(pedido(3, "A", null), 7, roles));

        assertTrue(ex.getMessage().contains("No puedes calificar"), ex.getMessage());
    }

    @Test
    void laPrimeraNotaRegistradaEsLaQueVale() {
        existente(1, "A");

        var ex = assertThrows(IllegalArgumentException.class, () -> service.registrar(pedido(1, "AD", null), 7, roles));

        assertTrue(ex.getMessage().contains("ya la registró ROSA"), ex.getMessage());
        verify(notaRepository, never()).save(any());
    }

    @Test
    void corregirCambiaLaNotaYaRegistrada() {
        Nota nota = existente(1, "A");

        service.corregir(pedido(1, "AD", null), 7, roles);

        assertEquals("AD", nota.getCalificacion());
    }

    @Test
    void conBoCExigeConclusion() {
        var ex = assertThrows(IllegalArgumentException.class, () -> service.registrar(pedido(1, "B", null), 7, roles));
        assertTrue(ex.getMessage().contains("B o C"), ex.getMessage());

        assertThrows(IllegalArgumentException.class, () -> service.registrar(pedido(1, "C", "corta"), 7, roles));
    }

    @Test
    void enVigesimalExigeConclusionConDoceOMenos() {
        config.setEscala(EscalaCalificacion.VIGESIMAL);

        assertThrows(IllegalArgumentException.class, () -> service.registrar(pedido(1, "12", " "), 7, roles));
        service.registrar(pedido(2, "13", null), 7, roles);
        verify(notaRepository).save(any());
    }

    @Test
    void conElPeriodoCerradoPideCodigo() {
        abrirPeriodo(false);
        doThrow(new AutorizacionRequeridaException("Falta el código"))
                .when(notaService).consumirAutorizacion(null, 7);

        assertThrows(AutorizacionRequeridaException.class, () -> service.registrar(pedido(1, "A", null), 7, roles));
        verify(notaRepository, never()).save(any());
    }

    @Test
    void soloEnUnAnioVigente() {
        anio.setEstado(AnioEscolarService.ESTADO_CERRADO);

        var ex = assertThrows(IllegalArgumentException.class, () -> service.registrar(pedido(1, "A", null), 7, roles));

        assertTrue(ex.getMessage().contains("año vigente"));
    }

    @Test
    void laGuiaIncluyeLasEvaluacionesDeOtrosSubcursosYAvisaLosQueFaltan() {
        Evaluacion deGeometria = new Evaluacion();
        deGeometria.setIdEvaluacion(70);
        deGeometria.setNombre("Práctica 1");
        deGeometria.setFecha(hoy);
        deGeometria.setAsignacion(asignacion(11, geometria, new Docente()));
        deGeometria.setCompetencias(Set.of(c1));
        NotaEvaluacion nota = new NotaEvaluacion();
        nota.setEvaluacion(deGeometria);
        nota.setMatricula(ana);
        nota.setCalificacion("B");
        when(evaluacionRepository.findByAsignacionGradoSeccionIdGradoSeccionAndPeriodoAndAccesoNot(30, (byte) 1, eliminado))
                .thenReturn(List.of(deGeometria));
        when(notaEvaluacionRepository.findByEvaluacionIdEvaluacionInAndAccesoNot(any(), eq(eliminado))).thenReturn(List.of(nota));

        NotaFinalResponse.Detalle detalle = service.detalle(30, 100, 1, 7, roles);

        var guia = detalle.alumnos().get(0).notas().get(0).guia();
        assertEquals("Geometría", guia.get(0).curso());
        assertEquals("B", guia.get(0).calificacion());
        assertEquals(List.of("Aritmética"), detalle.competencias().get(0).sinEvaluaciones());
    }

    @Test
    void sinElPermisoDeRegistrarLaPantallaEsDeSoloLectura() {
        assertFalse(service.detalle(30, 100, 1, 7, roles).puedeRegistrar());

        when(accesoContextoService.tieneAccion("NOTAS", "CREAR", 7, roles)).thenReturn(true);
        NotaFinalResponse.Detalle conPermiso = service.detalle(30, 100, 1, 7, roles);

        assertTrue(conPermiso.puedeRegistrar());
        assertFalse(conPermiso.puedeCorregir());
    }

    @Test
    void conElPeriodoPorComenzarNadieRegistraAunqueTengaPermiso() {
        periodo.setFechaInicio(hoy.plusDays(1));
        periodo.setFechaFin(hoy.plusDays(30));
        when(accesoContextoService.tieneAccion(any(), any(), any(), any())).thenReturn(true);

        NotaFinalResponse.Detalle detalle = service.detalle(30, 100, 1, 7, roles);

        assertFalse(detalle.periodoEditable());
        assertFalse(detalle.puedeRegistrar());
        assertFalse(detalle.puedeCorregir());
    }

    @Test
    void promedioVigesimalRedondeaDesdeCincoDecimas() {
        assertEquals(13, Calificaciones.promedio(List.of("12", "13")));
        assertEquals(12, Calificaciones.promedio(List.of("12", "12", "13")));
        assertNull(Calificaciones.promedio(List.of()));
    }
}
