package com.example.Escolar.Service;

import com.example.Escolar.Dto.ConfiguracionAcademicaResponse;
import com.example.Escolar.Dto.NotaFinalRequest;
import com.example.Escolar.Dto.NotaFinalResponse;
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
    @Mock BloqueoCalificaciones bloqueo;
    @Mock HistorialMatriculaRepository historialRepository;
    @Mock ReconocimientoCalificacionRepository reconocimientoRepository;
    @Mock NotaFinalAnualRepository notaAnualRepository;
    @InjectMocks NotaFinalService service;

    private final Acceso activo = new Acceso(AccesoConstants.ACTIVO, "Activo");
    private final Acceso eliminado = new Acceso(AccesoConstants.ELIMINADO, "Eliminado");
    private final Integer idRolActivo = 1;
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
        when(accesoContextoService.alcance(any(), any(), eq(idRolActivo))).thenReturn(AccesoContextoService.Alcance.DOCENTE);
        when(accesoContextoService.alcance(any(), any(), any(), eq(idRolActivo))).thenReturn(AccesoContextoService.Alcance.DOCENTE);
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
        when(accesoContextoService.asignacionesActivasDeDocente(7)).thenReturn(List.of(suya));
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
        return new NotaFinalRequest(30, 100, 1,
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
        List<NotaFinalResponse.Curso> cursos = service.cursos(2, 7, idRolActivo);

        assertEquals(1, cursos.size());
        assertEquals("MATEMÁTICA", cursos.get(0).area());
        assertEquals(List.of("01", "02"), cursos.get(0).competencias().stream().map(c -> c.codigo()).toList());
    }

    /** Ana tiene esas notas en evaluaciones de esta sección que afectan C1 y C2. */
    private void conEvaluaciones(String... calificaciones) {
        List<Evaluacion> evaluaciones = new java.util.ArrayList<>();
        List<NotaEvaluacion> notas = new java.util.ArrayList<>();
        for (int i = 0; i < calificaciones.length; i++) {
            Evaluacion e = new Evaluacion();
            e.setIdEvaluacion(60 + i);
            e.setNombre("Práctica " + i);
            e.setFecha(hoy);
            e.setAsignacion(asignacion(10, aritmetica, docenteAritmetica));
            e.setCompetencias(Set.of(c1, c2));
            NotaEvaluacion n = new NotaEvaluacion();
            n.setEvaluacion(e);
            n.setMatricula(ana);
            n.setCalificacion(calificaciones[i]);
            evaluaciones.add(e);
            notas.add(n);
        }
        when(evaluacionRepository.findByAsignacionGradoSeccionIdGradoSeccionAndPeriodoAndAccesoNot(30, (byte) 1, eliminado))
                .thenReturn(evaluaciones);
        when(notaEvaluacionRepository.findByEvaluacionIdEvaluacionInAndAccesoNot(any(), eq(eliminado))).thenReturn(notas);
    }

    private NotaFinalRequest correccion(int competencia, String calificacion, String motivo) {
        return new NotaFinalRequest(30, 100, 1,
                List.of(new NotaFinalRequest.Item(1, competencia, calificacion, null)), motivo);
    }

    @Test
    void registraUnaNotaNueva() {
        conEvaluaciones("A");
        service.registrar(pedido(1, "a", null), 7, idRolActivo);

        ArgumentCaptor<Nota> captor = ArgumentCaptor.forClass(Nota.class);
        verify(notaRepository).save(captor.capture());
        assertEquals("A", captor.getValue().getCalificacion());
        assertEquals(1, captor.getValue().getBimestre());
    }

    @Test
    void noCalificaUnaCompetenciaDeOtroSubcurso() {
        var ex = assertThrows(IllegalArgumentException.class, () -> service.registrar(pedido(3, "A", null), 7, idRolActivo));

        assertTrue(ex.getMessage().contains("No puedes calificar"), ex.getMessage());
    }

    @Test
    void laPrimeraNotaRegistradaEsLaQueVale() {
        existente(1, "A");

        var ex = assertThrows(IllegalArgumentException.class, () -> service.registrar(pedido(1, "AD", null), 7, idRolActivo));

        assertTrue(ex.getMessage().contains("ya la registró ROSA"), ex.getMessage());
        verify(notaRepository, never()).save(any());
    }

    @Test
    void corregirGuardaLaAnteriorConSuMotivo() {
        Nota anterior = existente(1, "A");

        var ex = assertThrows(IllegalArgumentException.class, () -> service.corregir(pedido(1, "AD", null), 7, idRolActivo));
        assertTrue(ex.getMessage().contains("motivo"), ex.getMessage());

        service.corregir(correccion(1, "AD", "Error al digitar"), 7, idRolActivo);

        assertEquals("A", anterior.getCalificacion());
        assertEquals(eliminado, anterior.getAcceso());
        assertEquals("Error al digitar", anterior.getMotivoAnulacion());
        assertNotNull(anterior.getFechaAnulacion());
        ArgumentCaptor<Nota> captor = ArgumentCaptor.forClass(Nota.class);
        verify(notaRepository, times(2)).save(captor.capture());
        assertEquals("AD", captor.getAllValues().get(1).getCalificacion());
    }

    @Test
    void sinEvaluacionesNoHayNotaFinal() {
        var ex = assertThrows(IllegalArgumentException.class, () -> service.registrar(pedido(1, "A", null), 7, idRolActivo));

        assertTrue(ex.getMessage().contains("no tiene notas de evaluaciones"), ex.getMessage());
        verify(notaRepository, never()).save(any());
    }

    @Test
    void enVigesimalLaNotaEsElPromedioYSeIgnoraLaEnviada() {
        config.setEscala(EscalaCalificacion.VIGESIMAL);
        conEvaluaciones("14", "17");

        service.registrar(pedido(1, "05", null), 7, idRolActivo);

        ArgumentCaptor<Nota> captor = ArgumentCaptor.forClass(Nota.class);
        verify(notaRepository).save(captor.capture());
        assertEquals("16", captor.getValue().getCalificacion());
    }

    @Test
    void enVigesimalCorregirSoloCambiaLaConclusion() {
        config.setEscala(EscalaCalificacion.VIGESIMAL);
        conEvaluaciones("11");
        existente(1, "11").setConclusionDescriptiva("Conclusión anterior del alumno en la competencia");

        service.corregir(new NotaFinalRequest(30, 100, 1, List.of(new NotaFinalRequest.Item(1, 1, "20",
                "Nueva conclusión descriptiva del alumno en la competencia")), "Ajuste"), 7, idRolActivo);

        ArgumentCaptor<Nota> captor = ArgumentCaptor.forClass(Nota.class);
        verify(notaRepository, times(2)).save(captor.capture());
        assertEquals("11", captor.getAllValues().get(1).getCalificacion());
    }

    private com.example.Escolar.Dto.ReaperturaNotaRequest reapertura() {
        return new com.example.Escolar.Dto.ReaperturaNotaRequest(30, 100, 1, 1, 1, "Nota mal calculada");
    }

    @Test
    void reabrirAnulaConMotivo() {
        Nota nota = existente(1, "A");

        service.reabrir(reapertura(), 7, idRolActivo);

        assertEquals(eliminado, nota.getAcceso());
        assertEquals("Nota mal calculada", nota.getMotivoAnulacion());
    }

    @Test
    void conNotaDelAnioNoSeReabreNiCorrige() {
        Nota nota = existente(1, "A");
        NotaFinalAnual anual = new NotaFinalAnual();
        anual.setMatricula(ana);
        anual.setCompetencia(c1);
        when(notaAnualRepository.findByMatriculaIdMatriculaInAndCompetenciaIdCompetenciaInAndAccesoNot(any(), any(), eq(eliminado)))
                .thenReturn(List.of(anual));

        assertThrows(IllegalArgumentException.class, () -> service.reabrir(reapertura(), 7, idRolActivo));
        assertThrows(IllegalArgumentException.class, () -> service.corregir(correccion(1, "AD", "x"), 7, idRolActivo));
        assertEquals(activo, nota.getAcceso() == null ? activo : nota.getAcceso());
        verify(notaRepository, never()).save(any());
    }

    @Test
    void conBoCExigeConclusion() {
        conEvaluaciones("B");
        var ex = assertThrows(IllegalArgumentException.class, () -> service.registrar(pedido(1, "B", null), 7, idRolActivo));
        assertTrue(ex.getMessage().contains("B o C"), ex.getMessage());

        assertThrows(IllegalArgumentException.class, () -> service.registrar(pedido(1, "C", "corta"), 7, idRolActivo));
    }

    @Test
    void enVigesimalExigeConclusionConDoceOMenos() {
        config.setEscala(EscalaCalificacion.VIGESIMAL);
        conEvaluaciones("12");

        assertThrows(IllegalArgumentException.class, () -> service.registrar(pedido(1, "20", " "), 7, idRolActivo));
        verify(notaRepository, never()).save(any());
    }

    @Test
    void conElPeriodoCerradoSoloRegistraQuienCorrige() {
        abrirPeriodo(false);
        conEvaluaciones("A");

        assertFalse(service.detalle(30, 100, 1, 7, idRolActivo).puedeRegistrar());
        var ex = assertThrows(IllegalArgumentException.class, () -> service.registrar(pedido(1, "A", null), 7, idRolActivo));
        assertTrue(ex.getMessage().contains("ya cerró"), ex.getMessage());
        verify(notaRepository, never()).save(any());

        service.corregir(pedido(1, "A", null), 7, idRolActivo);
        verify(notaRepository).save(any());
    }

    @Test
    void soloEnUnAnioVigente() {
        anio.setEstado(AnioEscolarService.ESTADO_CERRADO);

        var ex = assertThrows(IllegalArgumentException.class, () -> service.registrar(pedido(1, "A", null), 7, idRolActivo));

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

        NotaFinalResponse.Detalle detalle = service.detalle(30, 100, 1, 7, idRolActivo);

        var guia = detalle.alumnos().get(0).notas().get(0).guia();
        assertEquals("Geometría", guia.get(0).curso());
        assertEquals("B", guia.get(0).calificacion());
        assertEquals(List.of("Aritmética"), detalle.competencias().get(0).sinEvaluaciones());
    }

    @Test
    void sinElPermisoDeRegistrarLaPantallaEsDeSoloLectura() {
        assertFalse(service.detalle(30, 100, 1, 7, idRolActivo).puedeRegistrar());

        when(accesoContextoService.tieneAccion("NOTAS", "CREAR", 7, idRolActivo)).thenReturn(true);
        NotaFinalResponse.Detalle conPermiso = service.detalle(30, 100, 1, 7, idRolActivo);

        assertTrue(conPermiso.puedeRegistrar());
        assertFalse(conPermiso.puedeCorregir());
    }

    @Test
    void conElPeriodoPorComenzarNadieRegistraAunqueTengaPermiso() {
        periodo.setFechaInicio(hoy.plusDays(1));
        periodo.setFechaFin(hoy.plusDays(30));
        when(accesoContextoService.tieneAccion(any(), any(), any(), eq(idRolActivo))).thenReturn(true);

        NotaFinalResponse.Detalle detalle = service.detalle(30, 100, 1, 7, idRolActivo);

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

    // ── Alumnos trasladados ──

    private GradoSeccion otraSeccion() {
        Seccion b = new Seccion();
        b.setNombre("B");
        GradoSeccion otra = new GradoSeccion();
        otra.setIdGradoSeccion(31);
        otra.setGrado(gs.getGrado());
        otra.setSeccion(b);
        otra.setTurno(gs.getTurno());
        otra.setAnioEscolar(anio);
        return otra;
    }

    private HistorialMatricula tramo(GradoSeccion seccion, LocalDate inicio, LocalDate fin) {
        HistorialMatricula h = new HistorialMatricula();
        h.setMatricula(ana);
        h.setGradoSeccion(seccion);
        h.setFechaInicio(inicio);
        h.setFechaFinal(fin);
        return h;
    }

    /** Ana estuvo en 2do B hasta ese día y desde entonces está en esta sección. */
    private void llegoEl(LocalDate llegada) {
        when(historialRepository.findByMatriculaIdMatriculaInAndAccesoNot(any(), eq(eliminado))).thenReturn(List.of(
                tramo(otraSeccion(), hoy.minusDays(60), llegada), tramo(gs, llegada, null)));
    }

    @Test
    void laNotaFinalPuestaEnOtraSeccionLoIndica() {
        llegoEl(hoy.minusDays(3));
        existente(1, "A").setFechaRegistro(hoy.minusDays(10));

        var celda = service.detalle(30, 100, 1, 7, idRolActivo).alumnos().get(0).notas().get(0);

        assertEquals("2do B · Mañana", celda.seccionOrigen());
        assertEquals("ROSA DIAZ", celda.registradoPor());
    }

    @Test
    void laNotaFinalPuestaAquiNoLlevaEtiqueta() {
        llegoEl(hoy.minusDays(3));
        existente(1, "A").setFechaRegistro(hoy);

        assertNull(service.detalle(30, 100, 1, 7, idRolActivo).alumnos().get(0).notas().get(0).seccionOrigen());
    }

    private NotaEvaluacion notaDeOtraSeccion() {
        Asignacion anterior = asignacion(20, aritmetica, new Docente());
        anterior.setGradoSeccion(otraSeccion());
        Evaluacion e = new Evaluacion();
        e.setIdEvaluacion(80);
        e.setNombre("Examen B");
        e.setFecha(hoy.minusDays(8));
        e.setPeriodo((byte) 1);
        e.setAsignacion(anterior);
        e.setCompetencias(Set.of(c1));
        e.setAcceso(activo);
        NotaEvaluacion n = new NotaEvaluacion();
        n.setIdNotaEvaluacion(81);
        n.setEvaluacion(e);
        n.setMatricula(ana);
        n.setCalificacion("AD");
        when(notaEvaluacionRepository.findByMatriculaIdMatriculaInAndEvaluacionPeriodoAndAccesoNot(any(), eq((byte) 1), eq(eliminado)))
                .thenReturn(List.of(n));
        return n;
    }

    @Test
    void lasNotasDeOtraSeccionSeVenComoReferenciaSinEntrarAlPromedio() {
        config.setEscala(EscalaCalificacion.VIGESIMAL);
        notaDeOtraSeccion().setCalificacion("18");

        var celda = service.detalle(30, 100, 1, 7, idRolActivo).alumnos().get(0).notas().get(0);

        assertEquals(1, celda.referencias().size());
        assertEquals("2do B · Mañana", celda.referencias().get(0).seccion());
        assertEquals("18", celda.referencias().get(0).calificacion());
        assertNull(celda.promedio());
        // C2 no la cubre esa nota.
        assertTrue(service.detalle(30, 100, 1, 7, idRolActivo).alumnos().get(0).notas().get(1).referencias().isEmpty());
    }

    @Test
    void laNotaYaUsadaEnEstaSeccionNoSeRepiteComoReferencia() {
        notaDeOtraSeccion();
        Evaluacion aqui = new Evaluacion();
        aqui.setIdEvaluacion(90);
        aqui.setAsignacion(asignacion(10, aritmetica, docenteAritmetica));
        aqui.setCompetencias(Set.of(c1));
        when(evaluacionRepository.findByAsignacionGradoSeccionIdGradoSeccionAndPeriodoAndAccesoNot(30, (byte) 1, eliminado))
                .thenReturn(List.of(aqui));
        var uso = mock(ReconocimientoCalificacionRepository.Movimiento.class);
        when(uso.getVigencia()).thenReturn(ReconocimientoCalificacion.VIGENTE);
        when(uso.getOrigen()).thenReturn(81);
        when(uso.getDestino()).thenReturn(91);
        when(uso.getEvaluacion()).thenReturn(90);
        when(reconocimientoRepository.movimientosDe(any())).thenReturn(List.of(uso));

        assertTrue(service.detalle(30, 100, 1, 7, idRolActivo).alumnos().get(0).notas().get(0).referencias().isEmpty());
    }

    @Test
    void periodoCerradoCursadoEnOtraSeccionLoRegistraSoloDireccion() {
        abrirPeriodo(false);
        llegoEl(hoy);

        assertTrue(service.detalle(30, 100, 1, 7, idRolActivo).alumnos().get(0).notas().get(0).soloDireccion());
        var ex = assertThrows(IllegalArgumentException.class, () -> service.registrar(pedido(1, "A", null), 7, idRolActivo));
        assertTrue(ex.getMessage().contains("ya cerró"), ex.getMessage());
        verify(notaRepository, never()).save(any());

        service.corregir(pedido(1, "A", null), 7, idRolActivo);
        verify(notaRepository).save(any());
    }

    @Test
    void siLlegoDuranteElPeriodoNoVaAPendienteDeDireccion() {
        abrirPeriodo(false);
        llegoEl(hoy.minusDays(10));

        conEvaluaciones("A");
        assertFalse(service.detalle(30, 100, 1, 7, idRolActivo).alumnos().get(0).notas().get(0).soloDireccion());
        service.corregir(pedido(1, "A", null), 7, idRolActivo);
        verify(notaRepository).save(any());
    }

    @Test
    void avisaLasNotasFinalesQueFaltanAntesDelCambioDeSeccion() {
        abrirPeriodo(false);
        when(matriculaRepository.findById(1)).thenReturn(Optional.of(ana));
        when(cursoRepository.findByAnioEscolarIdAnioAndNivelIdNivelAndAccesoNot(2, 3, eliminado)).thenReturn(List.of(matematica));
        when(competenciaRepository.findByCursoIdCursoAndAccesoNot(100, eliminado)).thenReturn(List.of(c1, c2));
        Nota soloC1 = new Nota();
        soloC1.setBimestre((byte) 1);
        soloC1.setCompetencia(c1);
        when(notaRepository.findByMatriculaIdMatriculaAndAccesoNot(1, eliminado)).thenReturn(List.of(soloC1));

        List<String> pendientes = service.pendientesTraslado(1);

        assertEquals(1, pendientes.size());
        assertTrue(pendientes.get(0).startsWith("MATEMÁTICA: "), pendientes.get(0));

        Nota c2Tambien = new Nota();
        c2Tambien.setBimestre((byte) 1);
        c2Tambien.setCompetencia(c2);
        when(notaRepository.findByMatriculaIdMatriculaAndAccesoNot(1, eliminado)).thenReturn(List.of(soloC1, c2Tambien));
        assertTrue(service.pendientesTraslado(1).isEmpty());
    }
}
