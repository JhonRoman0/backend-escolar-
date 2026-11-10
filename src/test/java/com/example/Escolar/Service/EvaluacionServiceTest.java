package com.example.Escolar.Service;

import com.example.Escolar.Dto.ConfiguracionAcademicaResponse;
import com.example.Escolar.Dto.EvaluacionRequest;
import com.example.Escolar.Dto.NotasEvaluacionRequest;
import com.example.Escolar.Dto.PeriodoEvaluacionResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EvaluacionServiceTest {
    @Test void contadorIncluyeReconocidaVigentePeroNoMatriculasInactivas() {
        Evaluacion e = evaluacionGuardada();
        Matricula actual = matricula(1), inactiva = matricula(2), borrada = matricula(3);
        inactiva.setAcceso(new Acceso(AccesoConstants.INACTIVO, "Inactivo"));
        borrada.setAcceso(eliminado);
        when(matriculaRepository.findByGradoSeccionIdGradoSeccionAndAccesoNot(30,eliminado)).thenReturn(List.of(actual,inactiva,borrada));
        when(evaluacionRepository.findByAsignacionIdAsignacionAndPeriodoAndAccesoNotOrderByFechaAscIdEvaluacionAsc(10,(byte)1,eliminado)).thenReturn(List.of(e));
        // Una reconocida vigente tiene la misma representacion en nota_evaluacion.
        NotaEvaluacion reconocida = new NotaEvaluacion(); reconocida.setMatricula(actual); reconocida.setEvaluacion(e); reconocida.setCalificacion("A");
        NotaEvaluacion antigua = new NotaEvaluacion(); antigua.setMatricula(inactiva); antigua.setEvaluacion(e); antigua.setCalificacion("B");
        when(notaRepository.findByEvaluacionIdEvaluacionInAndAccesoNot(any(),eq(eliminado))).thenReturn(List.of(reconocida,antigua));
        var resumen = service.listar(10,1,7,roles).get(0);
        assertEquals(1,resumen.notasRegistradas()); assertEquals(1,resumen.totalAlumnos());
        // La consulta utilizada excluye las notas eliminadas, incluidas las anuladas.
        verify(notaRepository).findByEvaluacionIdEvaluacionInAndAccesoNot(any(),eq(eliminado));
        verifyNoInteractions(reconocimientos);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {0, 1, 2})
    void contadorSoloIncluyeMatriculasActualesYConservaBloqueoHistorico(int actuales) {
        Evaluacion e = evaluacionGuardada();
        when(evaluacionRepository.findByAsignacionIdAsignacionAndPeriodoAndAccesoNotOrderByFechaAscIdEvaluacionAsc(10,(byte)1,eliminado)).thenReturn(List.of(e));
        when(matriculaRepository.findByGradoSeccionIdGradoSeccionAndAccesoNot(30,eliminado)).thenReturn(java.util.stream.IntStream.rangeClosed(1,actuales).mapToObj(this::matricula).toList());
        NotaEvaluacion n = new NotaEvaluacion(); n.setEvaluacion(e); n.setMatricula(matricula(1)); n.setCalificacion("A");
        NotaEvaluacion historica = new NotaEvaluacion(); historica.setEvaluacion(e); historica.setMatricula(matricula(99)); historica.setCalificacion("B");
        when(notaRepository.findByEvaluacionIdEvaluacionInAndAccesoNot(any(),eq(eliminado))).thenReturn(List.of(n,historica));
        var listado = service.listar(10,1,7,roles).get(0);
        var registro = service.registro(10,1,7,roles).evaluaciones().get(0);
        assertEquals(actuales == 0 ? 0 : 1,listado.notasRegistradas());
        assertEquals(actuales,listado.totalAlumnos());
        assertEquals(listado.notasRegistradas(),registro.notasRegistradas());
        assertTrue(listado.tieneNotasRegistradas());
        verify(notaRepository,never()).save(any());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {1, 30})
    void lecturasDeHistorialNoSeMultiplicanPorAlumno(int cantidad) {
        evaluacionGuardada();
        when(matriculaRepository.findByGradoSeccionIdGradoSeccionAndAccesoNot(30, eliminado))
            .thenReturn(java.util.stream.IntStream.rangeClosed(1, cantidad).mapToObj(this::matricula).toList());
        service.detalle(50, 7, roles);
        verify(reconocimientos, times(1)).movimientosDe(any());
        verify(notaRepository, times(1)).alumnosConHistorial(any(), any(), any(), any(), any());
        verify(notaRepository, times(1)).findByEvaluacionIdEvaluacion(50);
        verify(reconocimientos, never()).findByNotaOrigenMatriculaIdMatricula(any());
    }
    @Mock EvaluacionRepository evaluacionRepository;
    @Mock NotaEvaluacionRepository notaRepository;
    @Mock NotaRepository notaFinalRepository;
    @Mock AsignacionRepository asignacionRepository;
    @Mock CompetenciaRepository competenciaRepository;
    @Mock MatriculaRepository matriculaRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock AccesoRepository accesoRepository;
    @Mock AccesoContextoService accesoContextoService;
    @Mock ConfiguracionAcademicaService configuracionService;
    @Mock HorarioClaseRepository horarioRepository;
    @Mock DiaFeriadoRepository feriadoRepository;
    @Mock BloqueoCalificaciones bloqueo;
    @Mock com.example.Escolar.Repository.ReconocimientoCalificacionRepository reconocimientos;
    @InjectMocks EvaluacionService service;

    private final Acceso activo = new Acceso(AccesoConstants.ACTIVO, "Activo");
    private final Acceso eliminado = new Acceso(AccesoConstants.ELIMINADO, "Eliminado");
    private final List<String> roles = List.of("Docente");
    private final LocalDate hoy = LocalDate.now();
    private AnioEscolar anio;
    private Asignacion asignacion;
    private Competencia c1;
    private Competencia c2;
    private Competencia c3;
    private ConfiguracionAcademicaResponse config;

    @BeforeEach
    void preparar() {
        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(activo));
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(new Usuario()));
        anio = new AnioEscolar();
        anio.setIdAnio(2);
        anio.setEstado(AnioEscolarService.ESTADO_VIGENTE);
        Nivel nivel = new Nivel();
        nivel.setIdNivel(2);
        Grado grado = new Grado();
        grado.setNivel(nivel);
        GradoSeccion gs = new GradoSeccion();
        gs.setIdGradoSeccion(30);
        gs.setGrado(grado);
        c1 = competencia(1, "01");
        c2 = competencia(2, "02");
        c3 = competencia(3, "03");
        Curso area = new Curso();
        area.setNombre("MATEMÁTICA");
        Curso geometria = new Curso();
        geometria.setNombre("Geometría");
        geometria.setTipo(TipoCurso.SUBCURSO);
        geometria.setCursoPadre(area);
        geometria.setCompetencias(Set.of(c1, c3));
        Docente docente = new Docente();
        docente.setIdDocente(5);
        asignacion = new Asignacion();
        asignacion.setIdAsignacion(10);
        asignacion.setCurso(geometria);
        asignacion.setGradoSeccion(gs);
        asignacion.setAnioEscolar(anio);
        asignacion.setDocente(docente);
        asignacion.setAcceso(activo);

        when(accesoContextoService.docenteDeUsuario(7)).thenReturn(Optional.of(docente));
        when(asignacionRepository.findByDocenteIdDocenteAndAccesoNot(5, eliminado)).thenReturn(List.of(asignacion));
        when(evaluacionRepository.save(any(Evaluacion.class))).thenAnswer(i -> i.getArgument(0));
        config = new ConfiguracionAcademicaResponse();
        config.setEscala(EscalaCalificacion.LITERAL);
        config.setTipoPeriodo(TipoPeriodo.BIMESTRE);
        config.setPeriodos(List.of(new PeriodoEvaluacionResponse(1, "Primer bimestre", hoy.minusDays(10), hoy.plusDays(10), "ABIERTO")));
        when(configuracionService.obtener(2, 2)).thenReturn(config);
        periodo(hoy.minusDays(10), hoy.plusDays(10));
        // Por defecto el curso se dicta el día de hoy, sea cual sea.
        horario(hoy.getDayOfWeek().getValue());
    }

    private void horario(Integer... dias) {
        List<HorarioClase> horarios = new java.util.ArrayList<>();
        for (Integer dia : dias) {
            HorarioClase h = new HorarioClase();
            h.setDiaSemana(dia.byteValue());
            horarios.add(h);
        }
        when(horarioRepository.findByAsignacionIdAsignacionAndAccesoNot(10, eliminado)).thenReturn(horarios);
    }

    /** Un día del periodo (hoy ± 10) que cae en el día de la semana pedido. */
    private LocalDate proximo(java.time.DayOfWeek dia) {
        return hoy.minusDays(6).with(java.time.temporal.TemporalAdjusters.nextOrSame(dia));
    }

    @Test
    void soloEnLosDiasQueSeDictaElCurso() {
        horario(1, 3);
        LocalDate martes = proximo(java.time.DayOfWeek.TUESDAY);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.crear(new EvaluacionRequest(10, 1, "Examen", martes, Set.of(1)), 7, roles));

        assertTrue(ex.getMessage().contains("se dicta lunes y miércoles"), ex.getMessage());
    }

    @Test
    void sinHorarioValeDeLunesAViernes() {
        horario();
        LocalDate sabado = proximo(java.time.DayOfWeek.SATURDAY);
        LocalDate jueves = proximo(java.time.DayOfWeek.THURSDAY);

        assertThrows(IllegalArgumentException.class,
                () -> service.crear(new EvaluacionRequest(10, 1, "Examen", sabado, Set.of(1)), 7, roles));
        service.crear(new EvaluacionRequest(10, 1, "Examen", jueves, Set.of(1)), 7, roles);
        verify(evaluacionRepository).save(any());
    }

    @Test
    void unFeriadoNoSePuedeElegir() {
        DiaFeriado feriado = new DiaFeriado();
        feriado.setFecha(hoy);
        feriado.setMotivo("Día de prueba");
        feriado.setAnioEscolar(anio);
        when(feriadoRepository.findByAccesoNot(eliminado)).thenReturn(List.of(feriado));

        var ex = assertThrows(IllegalArgumentException.class, () -> service.crear(request(1), 7, roles));

        assertTrue(ex.getMessage().contains("feriado"), ex.getMessage());
    }

    private Competencia competencia(int id, String codigo) {
        Competencia c = new Competencia();
        c.setIdCompetencia(id);
        c.setCodigo(codigo);
        c.setOrden((byte) id);
        c.setAcceso(activo);
        return c;
    }

    private void periodo(LocalDate inicio, LocalDate fin) {
        PeriodoEvaluacion p = new PeriodoEvaluacion();
        p.setNumero((byte) 1);
        p.setFechaInicio(inicio);
        p.setFechaFin(fin);
        when(configuracionService.periodos(any(), any())).thenReturn(List.of(p));
    }

    private EvaluacionRequest request(Integer... competencias) {
        return new EvaluacionRequest(10, 1, "Examen 1", hoy, Set.of(competencias));
    }

    @Test
    void creaConLasCompetenciasDelSubcurso() {
        service.crear(request(1, 3), 7, roles);

        ArgumentCaptor<Evaluacion> captor = ArgumentCaptor.forClass(Evaluacion.class);
        verify(evaluacionRepository).save(captor.capture());
        assertEquals(Set.of(c1, c3), captor.getValue().getCompetencias());
        assertEquals(1, captor.getValue().getPeriodo());
    }

    @Test
    void noAceptaUnaCompetenciaQueElSubcursoNoEvalua() {
        var ex = assertThrows(IllegalArgumentException.class, () -> service.crear(request(2), 7, roles));

        assertTrue(ex.getMessage().contains("Geometría"), ex.getMessage());
        verify(evaluacionRepository, never()).save(any());
    }

    @Test
    void unDocenteNoTrabajaConCursosAjenos() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.crear(new EvaluacionRequest(99, 1, "Examen", hoy, Set.of(1)), 7, roles));
    }

    @Test
    void soloEnUnAnioVigente() {
        anio.setEstado(AnioEscolarService.ESTADO_POR_COMENZAR);

        var ex = assertThrows(IllegalArgumentException.class, () -> service.crear(request(1), 7, roles));

        assertTrue(ex.getMessage().contains("año vigente"));
    }

    @Test
    void unPeriodoCerradoEsDeSoloLectura() {
        periodo(hoy.minusDays(40), hoy.minusDays(1));

        var ex = assertThrows(IllegalArgumentException.class, () -> service.crear(request(1), 7, roles));

        assertTrue(ex.getMessage().contains("ya cerró"), ex.getMessage());
    }

    @Test
    void laFechaDebeEstarDentroDelPeriodo() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.crear(new EvaluacionRequest(10, 1, "Examen", hoy.plusDays(30), Set.of(1)), 7, roles));

        assertTrue(ex.getMessage().contains("dentro del periodo"));
    }

    private Matricula matricula(int id) {
        Alumno a = new Alumno();
        a.setNombre("ANA");
        a.setApellidoPat("PEREZ" + id);
        a.setApellidoMat("RUIZ");
        AnioEscolar anioM = new AnioEscolar();
        anioM.setAnio("2027");
        Nivel nivel = new Nivel();
        Grado grado = new Grado();
        grado.setNivel(nivel);
        GradoSeccion gs = new GradoSeccion();
        gs.setGrado(grado);
        gs.setAnioEscolar(anioM);
        AlumnoApoderado vinculo = new AlumnoApoderado();
        vinculo.setAlumno(a);
        Matricula m = new Matricula();
        m.setIdMatricula(id);
        m.setAlumnoApoderado(vinculo);
        m.setGradoSeccion(gs);
        m.setAcceso(activo);
        return m;
    }

    private Evaluacion evaluacionGuardada() {
        Evaluacion e = new Evaluacion();
        e.setIdEvaluacion(50);
        e.setAsignacion(asignacion);
        e.setPeriodo((byte) 1);
        e.setNombre("Examen 1");
        e.setFecha(hoy);
        e.setUsuarioRegistro(new Usuario());
        e.setAcceso(activo);
        when(evaluacionRepository.findByIdEvaluacionAndAccesoNot(50, eliminado)).thenReturn(Optional.of(e));
        when(matriculaRepository.findByGradoSeccionIdGradoSeccionAndAccesoNot(30, eliminado))
                .thenReturn(List.of(matricula(1), matricula(2)));
        return e;
    }

    @Test
    void registraNotasEnLaEscalaDelNivel() {
        evaluacionGuardada();

        service.registrarNotas(50, new NotasEvaluacionRequest(List.of(new NotasEvaluacionRequest.Item(1, "ad"))), 7, roles);

        ArgumentCaptor<NotaEvaluacion> captor = ArgumentCaptor.forClass(NotaEvaluacion.class);
        verify(notaRepository).save(captor.capture());
        assertEquals("AD", captor.getValue().getCalificacion());
    }

    @Test
    void rechazaUnaNotaFueraDeLaEscala() {
        evaluacionGuardada();

        assertThrows(IllegalArgumentException.class, () -> service.registrarNotas(50,
                new NotasEvaluacionRequest(List.of(new NotasEvaluacionRequest.Item(1, "15"))), 7, roles));
        verify(notaRepository, never()).save(any());
    }

    @Test
    void dejarLaNotaVaciaLaBorra() {
        Evaluacion e = evaluacionGuardada();
        NotaEvaluacion existente = new NotaEvaluacion();
        existente.setEvaluacion(e);
        existente.setMatricula(matricula(1));
        existente.setCalificacion("B");
        existente.setAcceso(activo);
        when(notaRepository.findByEvaluacionIdEvaluacion(50)).thenReturn(List.of(existente));

        service.registrarNotas(50, new NotasEvaluacionRequest(List.of(new NotasEvaluacionRequest.Item(1, " "))), 7, roles);

        // Borrado lógico: la fila queda marcada, no desaparece.
        verify(notaRepository, never()).delete(any());
        assertEquals(AccesoConstants.ELIMINADO, existente.getAcceso().getIdAcceso());
    }

    @Test
    void noCalificaAAlguienDeOtraSeccion() {
        evaluacionGuardada();

        var ex = assertThrows(IllegalArgumentException.class, () -> service.registrarNotas(50,
                new NotasEvaluacionRequest(List.of(new NotasEvaluacionRequest.Item(99, "A"))), 7, roles));

        assertTrue(ex.getMessage().contains("ya no está matriculado"));
    }

    @Test
    void escalaLiteralSoloAceptaLetras() {
        assertEquals("AD", Calificaciones.normalizar(EscalaCalificacion.LITERAL, " ad "));
        assertThrows(IllegalArgumentException.class, () -> Calificaciones.normalizar(EscalaCalificacion.LITERAL, "D"));
    }

    @Test
    void escalaVigesimalSoloAceptaDe0A20() {
        assertEquals("7", Calificaciones.normalizar(EscalaCalificacion.VIGESIMAL, "07"));
        assertEquals("20", Calificaciones.normalizar(EscalaCalificacion.VIGESIMAL, "20"));
        for (String invalida : List.of("21", "-1", "12.5", "A", "")) {
            assertThrows(IllegalArgumentException.class, () -> Calificaciones.normalizar(EscalaCalificacion.VIGESIMAL, invalida));
        }
    }

    @Test
    void conNotasSoloSeCambiaElNombre() {
        Evaluacion e = evaluacionGuardada();
        e.setCompetencias(Set.of(c1));
        when(notaRepository.countByEvaluacionIdEvaluacionAndAccesoNot(50, eliminado)).thenReturn(3L);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.editar(50, new EvaluacionRequest(null, null, "Examen 1", hoy, Set.of(1, 3)), 7, roles));
        assertTrue(ex.getMessage().contains("solo se puede cambiar el nombre"));

        service.editar(50, new EvaluacionRequest(null, null, "Examen final", hoy, Set.of(1)), 7, roles);
        assertEquals("Examen final", e.getNombre());
    }

    @Test
    void conNotasNoSeElimina() {
        Evaluacion e = evaluacionGuardada();
        when(notaRepository.countByEvaluacionIdEvaluacionAndAccesoNot(50, eliminado)).thenReturn(1L);

        assertThrows(IllegalArgumentException.class, () -> service.eliminar(50, 7, roles));

        assertEquals(AccesoConstants.ACTIVO, e.getAcceso().getIdAcceso());
    }

    @Test
    void sinNotasSeEliminaCambiandoSoloSuEstado() {
        Evaluacion e = evaluacionGuardada();

        service.eliminar(50, 7, roles);

        assertEquals(AccesoConstants.ELIMINADO, e.getAcceso().getIdAcceso());
        verify(evaluacionRepository, never()).delete(any());
    }

    @Test
    void elRegistroAuxiliarJuntaLasNotasPorCompetencia() {
        config.setEscala(EscalaCalificacion.VIGESIMAL);
        Evaluacion e = evaluacionGuardada();
        e.setCompetencias(Set.of(c1));
        Matricula ana = matricula(1);
        NotaEvaluacion n = new NotaEvaluacion();
        n.setEvaluacion(e);
        n.setMatricula(ana);
        n.setCalificacion("15");
        when(evaluacionRepository.findByAsignacionIdAsignacionAndPeriodoAndAccesoNotOrderByFechaAscIdEvaluacionAsc(10, (byte) 1, eliminado))
                .thenReturn(List.of(e));
        when(notaRepository.findByEvaluacionIdEvaluacionInAndAccesoNot(any(), eq(eliminado))).thenReturn(List.of(n));

        var registro = service.registro(10, 1, 7, roles);

        var fila = registro.alumnos().stream().filter(f -> f.idMatricula() == 1).findFirst().orElseThrow();
        assertEquals("15", fila.notas().get(50));
        assertEquals(15, fila.competencias().get(0).promedio());
        assertEquals(List.of("15"), fila.competencias().get(0).notas());
    }

    private void notaFinalDe(int idMatricula, Competencia competencia) {
        Nota fin = new Nota();
        fin.setMatricula(matricula(idMatricula));
        fin.setCompetencia(competencia);
        when(notaFinalRepository.findByMatriculaGradoSeccionIdGradoSeccionAndBimestreAndAccesoNot(30, (byte) 1, eliminado))
                .thenReturn(List.of(fin));
    }

    @Test
    void notaReconocidaNoSeEditaPorRegistroOrdinario() {
        Evaluacion e = evaluacionGuardada();
        NotaEvaluacion n = new NotaEvaluacion(); n.setIdNotaEvaluacion(90); n.setEvaluacion(e);
        n.setMatricula(matricula(1)); n.setCalificacion("A"); n.setAcceso(activo);
        when(notaRepository.findByEvaluacionIdEvaluacion(50)).thenReturn(List.of(n));
        when(reconocimientos.existsByNotaDestinoIdNotaEvaluacion(90)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.registrarNotas(50,
            new NotasEvaluacionRequest(List.of(new NotasEvaluacionRequest.Item(1,"B"))),7,roles));
        verify(notaRepository,never()).save(any());
    }

    @Test
    void reconocidoParticipaUnaVezEnPromedioSinSumarHistorial() {
        config.setEscala(EscalaCalificacion.VIGESIMAL);
        Evaluacion nativa = evaluacionGuardada(); nativa.setCompetencias(Set.of(c1));
        Evaluacion reconocida = new Evaluacion(); reconocida.setIdEvaluacion(51); reconocida.setAsignacion(asignacion);
        reconocida.setCompetencias(Set.of(c1)); reconocida.setUsuarioRegistro(new Usuario());
        Matricula ana = matricula(1);
        NotaEvaluacion diez = new NotaEvaluacion(); diez.setEvaluacion(nativa); diez.setMatricula(ana); diez.setCalificacion("10");
        NotaEvaluacion veinte = new NotaEvaluacion(); veinte.setEvaluacion(reconocida); veinte.setMatricula(ana); veinte.setCalificacion("20");
        when(evaluacionRepository.findByAsignacionIdAsignacionAndPeriodoAndAccesoNotOrderByFechaAscIdEvaluacionAsc(10,(byte)1,eliminado))
            .thenReturn(List.of(nativa,reconocida));
        when(notaRepository.findByEvaluacionIdEvaluacionInAndAccesoNot(any(),eq(eliminado))).thenReturn(List.of(diez));
        assertEquals(10, service.registro(10,1,7,roles).alumnos().get(0).competencias().get(0).promedio());
        when(notaRepository.findByEvaluacionIdEvaluacionInAndAccesoNot(any(),eq(eliminado))).thenReturn(List.of(diez,veinte));
        var registro = service.registro(10,1,7,roles);
        assertEquals(15,registro.alumnos().get(0).competencias().get(0).promedio());
        assertEquals(2,registro.alumnos().get(0).competencias().get(0).notas().size());
        assertNull(registro.alumnos().get(1).competencias().get(0).promedio());
    }

    @Test
    void conNotaFinalNoSeCambiaLaNotaDeEseAlumno() {
        Evaluacion e = evaluacionGuardada();
        e.setCompetencias(Set.of(c1, c3));
        notaFinalDe(1, c3);

        var ex = assertThrows(IllegalArgumentException.class, () -> service.registrarNotas(50,
                new NotasEvaluacionRequest(List.of(new NotasEvaluacionRequest.Item(1, "A"))), 7, roles));

        assertTrue(ex.getMessage().contains("ya está registrada"), ex.getMessage());
        verify(notaRepository, never()).save(any());
    }

    @Test
    void losAlumnosSinNotaFinalSiguenCalificandose() {
        Evaluacion e = evaluacionGuardada();
        e.setCompetencias(Set.of(c1));
        notaFinalDe(1, c1);

        service.registrarNotas(50, new NotasEvaluacionRequest(List.of(new NotasEvaluacionRequest.Item(2, "B"))), 7, roles);

        verify(notaRepository).save(any());
        var detalle = service.detalle(50, 7, roles);
        assertTrue(detalle.alumnos().stream().filter(a -> a.idMatricula() == 1).findFirst().orElseThrow().notaFinalRegistrada());
        assertEquals(1, detalle.evaluacion().alumnosConNotaFinal());
    }

    @Test
    void unaNotaFinalDeOtraCompetenciaNoBloquea() {
        Evaluacion e = evaluacionGuardada();
        e.setCompetencias(Set.of(c1));
        notaFinalDe(1, c3);

        service.registrarNotas(50, new NotasEvaluacionRequest(List.of(new NotasEvaluacionRequest.Item(1, "A"))), 7, roles);

        verify(notaRepository).save(any());
    }

    private void finalesDeTodos(Competencia competencia) {
        when(matriculaRepository.findByGradoSeccionIdGradoSeccionAndAccesoNot(30, eliminado))
                .thenReturn(List.of(matricula(1), matricula(2)));
        List<Nota> finales = new java.util.ArrayList<>();
        for (int id : new int[]{1, 2}) {
            Nota fin = new Nota();
            fin.setMatricula(matricula(id));
            fin.setCompetencia(competencia);
            finales.add(fin);
        }
        when(notaFinalRepository.findByMatriculaGradoSeccionIdGradoSeccionAndBimestreAndAccesoNot(30, (byte) 1, eliminado))
                .thenReturn(finales);
    }

    @Test
    void noSeMarcaUnaCompetenciaConTodasSusNotasFinales() {
        finalesDeTodos(c1);

        var ex = assertThrows(IllegalArgumentException.class, () -> service.crear(request(1), 7, roles));

        assertTrue(ex.getMessage().contains("competencia 01"), ex.getMessage());
        verify(evaluacionRepository, never()).save(any());
    }

    @Test
    void conNotasFinalesParcialesSiSeCrea() {
        when(matriculaRepository.findByGradoSeccionIdGradoSeccionAndAccesoNot(30, eliminado))
                .thenReturn(List.of(matricula(1), matricula(2)));
        notaFinalDe(1, c1);

        service.crear(request(1), 7, roles);

        verify(evaluacionRepository).save(any());
    }

    @Test
    void elAvanceDiceQueCompetenciasEstanCompletas() {
        finalesDeTodos(c1);

        var avance = service.avance(10, 1, 7, roles);

        assertTrue(avance.stream().filter(a -> a.idCompetencia() == 1).findFirst().orElseThrow().completa());
        assertFalse(avance.stream().filter(a -> a.idCompetencia() == 3).findFirst().orElseThrow().completa());
    }

    @Test
    void renombrarNoFallaSiSuCompetenciaYaSeCompleto() {
        Evaluacion e = evaluacionGuardada();
        e.setCompetencias(new java.util.HashSet<>(Set.of(c1)));
        finalesDeTodos(c1);

        service.editar(50, new EvaluacionRequest(null, null, "Examen final", hoy, Set.of(1)), 7, roles);

        assertEquals("Examen final", e.getNombre());
    }
}
