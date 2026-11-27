package com.example.Escolar.Service;

import com.example.Escolar.Dto.ConfiguracionAcademicaResponse;
import com.example.Escolar.Dto.NotaAnualRequest;
import com.example.Escolar.Dto.NotaFinalRequest;
import com.example.Escolar.Dto.ReaperturaNotaRequest;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.NotaFinalAnualRepository;
import com.example.Escolar.Repository.NotaRepository;
import com.example.Escolar.Repository.UsuarioRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotaFinalAnualServiceTest {
    @Mock NotaFinalService notaFinalService;
    @Mock NotaRepository notaRepository;
    @Mock NotaFinalAnualRepository anualRepository;
    @Mock ConfiguracionAcademicaService configuracionService;
    @Mock UsuarioRepository usuarioRepository;
    @Mock AccesoRepository accesoRepository;
    @Mock AccesoContextoService accesoContextoService;
    @Mock BloqueoCalificaciones bloqueo;
    @InjectMocks NotaFinalAnualService service;

    private final Acceso activo = new Acceso(AccesoConstants.ACTIVO, "Activo");
    private final Acceso eliminado = new Acceso(AccesoConstants.ELIMINADO, "Eliminado");
    private final List<String> roles = List.of("Docente");
    private final LocalDate hoy = LocalDate.now();
    private final List<Nota> porPeriodo = new ArrayList<>();
    private AnioEscolar anio;
    private ConfiguracionAcademicaResponse config;
    private PeriodoEvaluacion b1;
    private PeriodoEvaluacion b2;
    private Competencia c1;
    private Matricula ana;

    @BeforeEach
    void preparar() {
        anio = new AnioEscolar();
        anio.setIdAnio(2);
        anio.setEstado(AnioEscolarService.ESTADO_VIGENTE);
        Nivel nivel = new Nivel();
        nivel.setIdNivel(3);
        Grado grado = new Grado();
        grado.setNivel(nivel);
        GradoSeccion gs = new GradoSeccion();
        gs.setIdGradoSeccion(30);
        gs.setGrado(grado);
        gs.setAnioEscolar(anio);
        c1 = new Competencia();
        c1.setIdCompetencia(1);
        c1.setCodigo("01");
        Curso area = new Curso();
        area.setIdCurso(100);
        area.setNombre("MATEMÁTICA");
        Alumno alumno = new Alumno();
        alumno.setNombre("ANA");
        alumno.setApellidoPat("PEREZ");
        AlumnoApoderado vinculo = new AlumnoApoderado();
        vinculo.setAlumno(alumno);
        ana = new Matricula();
        ana.setIdMatricula(1);
        ana.setAlumnoApoderado(vinculo);

        config = new ConfiguracionAcademicaResponse();
        config.setEscala(EscalaCalificacion.LITERAL);
        config.setTipoPeriodo(TipoPeriodo.BIMESTRE);
        b1 = periodo(1, hoy.minusDays(60), hoy.minusDays(31));
        b2 = periodo(2, hoy.minusDays(30), hoy.minusDays(1));

        when(notaFinalService.alcance(eq(30), eq(100), eq(7), eq(roles), eq("NOTAS_ANUALES"), any())).thenReturn(new NotaFinalService.Alcance(gs, area, List.of(c1)));
        when(notaFinalService.configuracion(gs)).thenReturn(config);
        when(notaFinalService.alumnosDe(any(), eq(7), eq(roles), eq("NOTAS_ANUALES"), any())).thenReturn(List.of(ana));
        when(notaFinalService.eliminado()).thenReturn(eliminado);
        when(configuracionService.periodos(anio, nivel)).thenAnswer(i -> List.of(b1, b2));
        when(notaRepository.findByMatriculaIdMatriculaInAndCompetenciaIdCompetenciaInAndAccesoNot(any(), any(), eq(eliminado)))
                .thenReturn(porPeriodo);
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(new Usuario()));
        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(activo));
    }

    private static PeriodoEvaluacion periodo(int numero, LocalDate inicio, LocalDate fin) {
        PeriodoEvaluacion p = new PeriodoEvaluacion();
        p.setNumero((byte) numero);
        p.setFechaInicio(inicio);
        p.setFechaFin(fin);
        return p;
    }

    private void notaDelPeriodo(int periodo, String calificacion) {
        Nota n = new Nota();
        n.setMatricula(ana);
        n.setCompetencia(c1);
        n.setBimestre((byte) periodo);
        n.setCalificacion(calificacion);
        porPeriodo.add(n);
    }

    private NotaAnualRequest pedido(String calificacion, String conclusion, String motivo) {
        return new NotaAnualRequest(30, 100, List.of(new NotaFinalRequest.Item(1, 1, calificacion, conclusion)), motivo);
    }

    private NotaFinalAnual guardada() {
        ArgumentCaptor<NotaFinalAnual> captor = ArgumentCaptor.forClass(NotaFinalAnual.class);
        verify(anualRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void noSePoneAntesDeQueTermineElUltimoPeriodo() {
        b2.setFechaFin(hoy.plusDays(5));
        notaDelPeriodo(1, "A");
        notaDelPeriodo(2, "A");

        var detalle = service.detalle(30, 100, 7, roles);
        assertFalse(detalle.disponible());
        assertTrue(detalle.noDisponible().contains("segundo bimestre"), detalle.noDisponible());
        assertThrows(IllegalArgumentException.class, () -> service.registrar(pedido("A", null, null), 7, roles));
        verify(anualRepository, never()).save(any());
    }

    @Test
    void exigeLasNotasDeTodosLosPeriodos() {
        notaDelPeriodo(1, "A");

        assertEquals(List.of("Segundo bimestre"), service.detalle(30, 100, 7, roles).alumnos().get(0).notas().get(0).faltan());
        var ex = assertThrows(IllegalArgumentException.class, () -> service.registrar(pedido("A", null, null), 7, roles));
        assertTrue(ex.getMessage().contains("Segundo bimestre"), ex.getMessage());
    }

    @Test
    void enLiteralSeEligeYConBoCPideConclusion() {
        notaDelPeriodo(1, "B");
        notaDelPeriodo(2, "A");

        assertThrows(IllegalArgumentException.class, () -> service.registrar(pedido("B", null, null), 7, roles));
        service.registrar(pedido("a", null, null), 7, roles);

        assertEquals("A", guardada().getCalificacion());
    }

    @Test
    void enVigesimalEsElPromedioDeLosPeriodosSinConclusion() {
        config.setEscala(EscalaCalificacion.VIGESIMAL);
        notaDelPeriodo(1, "14");
        notaDelPeriodo(2, "15");

        assertEquals(15, service.detalle(30, 100, 7, roles).alumnos().get(0).notas().get(0).promedio());
        service.registrar(pedido("02", "texto", null), 7, roles);

        assertEquals("15", guardada().getCalificacion());
        assertNull(guardada().getConclusionDescriptiva());
    }

    private NotaFinalAnual yaRegistrada(String calificacion) {
        notaDelPeriodo(1, "A");
        notaDelPeriodo(2, "A");
        NotaFinalAnual anual = new NotaFinalAnual();
        anual.setMatricula(ana);
        anual.setCompetencia(c1);
        anual.setCalificacion(calificacion);
        anual.setUsuarioRegistro(new Usuario());
        anual.setAcceso(activo);
        when(anualRepository.findByMatriculaIdMatriculaInAndCompetenciaIdCompetenciaInAndAccesoNot(any(), any(), eq(eliminado)))
                .thenReturn(List.of(anual));
        return anual;
    }

    @Test
    void laRegistradaSoloSeCambiaCorrigiendoConMotivo() {
        NotaFinalAnual anterior = yaRegistrada("A");

        assertThrows(IllegalArgumentException.class, () -> service.registrar(pedido("AD", null, null), 7, roles));
        assertThrows(IllegalArgumentException.class, () -> service.corregir(pedido("AD", null, null), 7, roles));
        service.corregir(pedido("AD", null, "Error al digitar"), 7, roles);

        assertEquals(eliminado, anterior.getAcceso());
        assertEquals("Error al digitar", anterior.getMotivoAnulacion());
        assertEquals("AD", guardada().getCalificacion());
    }

    @Test
    void reabrirLaAnulaConMotivo() {
        NotaFinalAnual anterior = yaRegistrada("A");

        service.reabrir(new ReaperturaNotaRequest(30, 100, null, 1, 1, "Bimestre corregido"), 7, roles);

        assertEquals(eliminado, anterior.getAcceso());
        assertEquals("Bimestre corregido", anterior.getMotivoAnulacion());
    }
}
