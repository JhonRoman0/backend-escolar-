package com.example.Escolar.Service;

import com.example.Escolar.Dto.BoletaResponse;
import com.example.Escolar.Dto.ConfiguracionAcademicaResponse;
import com.example.Escolar.Dto.PeriodoEvaluacionResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BoletaServiceTest {
    @Mock NotaRepository notaRepository;
    @Mock MatriculaRepository matriculaRepository;
    @Mock GradoSeccionRepository gradoSeccionRepository;
    @Mock AsistenciaAlumnoRepository asistenciaRepository;
    @Mock AccesoRepository accesoRepository;
    @Mock ConfiguracionAcademicaService configuracionService;
    @Mock NotaFinalService notaFinalService;
    @Mock MallaEvaluable mallaEvaluable;
    @InjectMocks BoletaService service;

    private final Acceso activo = new Acceso(AccesoConstants.ACTIVO, "Activo");
    private final Acceso eliminado = new Acceso(AccesoConstants.ELIMINADO, "Eliminado");
    private final List<String> roles = List.of("ADMIN");
    private final LocalDate hoy = LocalDate.now();
    private Competencia c1;
    private Matricula ana;

    @BeforeEach
    void preparar() {
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        AnioEscolar anio = new AnioEscolar();
        anio.setIdAnio(2);
        anio.setAnio("2027");
        Nivel nivel = new Nivel();
        nivel.setIdNivel(3);
        nivel.setNombre("Secundaria");
        Grado grado = new Grado();
        grado.setNombre("1ro");
        grado.setNivel(nivel);
        Turno turno = new Turno();
        turno.setNombre("Mañana");
        GradoSeccion gs = new GradoSeccion();
        gs.setIdGradoSeccion(26);
        gs.setGrado(grado);
        gs.setTurno(turno);
        gs.setAnioEscolar(anio);
        gs.setAcceso(activo);
        when(gradoSeccionRepository.findById(26)).thenReturn(Optional.of(gs));
        when(notaFinalService.puedeVerSeccion(any(), any(), any())).thenReturn(true);

        ConfiguracionAcademicaResponse config = new ConfiguracionAcademicaResponse();
        config.setEscala(EscalaCalificacion.LITERAL);
        config.setTipoPeriodo(TipoPeriodo.BIMESTRE);
        config.setPeriodos(List.of(
                new PeriodoEvaluacionResponse(1, "Primer bimestre", hoy.minusDays(80), hoy.minusDays(41), "CERRADO"),
                new PeriodoEvaluacionResponse(2, "Segundo bimestre", hoy.minusDays(40), hoy.plusDays(10), "ABIERTO"),
                new PeriodoEvaluacionResponse(3, "Tercer bimestre", hoy.plusDays(11), hoy.plusDays(50), "POR_COMENZAR")));
        when(configuracionService.obtener(2, 3)).thenReturn(config);
        PeriodoEvaluacion segundo = new PeriodoEvaluacion();
        segundo.setNumero((byte) 2);
        segundo.setFechaInicio(hoy.minusDays(40));
        segundo.setFechaFin(hoy.plusDays(10));
        when(configuracionService.periodos(any(), any())).thenReturn(List.of(segundo));

        c1 = new Competencia();
        c1.setIdCompetencia(11);
        c1.setCodigo("01");
        c1.setNombre("Resuelve problemas de cantidad");
        Curso mate = new Curso();
        mate.setNombre("MATEMÁTICA");
        mate.setTipo(TipoCurso.AREA);
        when(mallaEvaluable.areas(any(), any())).thenReturn(List.of(new MallaEvaluable.Area(mate, List.of(c1))));

        Alumno a = new Alumno();
        a.setNombre("ANA");
        a.setApellidoPat("ALVA");
        a.setApellidoMat("RUIZ");
        a.setCodigo("0001");
        AlumnoApoderado vinculo = new AlumnoApoderado();
        vinculo.setAlumno(a);
        AnioEscolar anioM = new AnioEscolar();
        anioM.setAnio("2027");
        GradoSeccion gsM = new GradoSeccion();
        gsM.setGrado(grado);
        gsM.setAnioEscolar(anioM);
        ana = new Matricula();
        ana.setIdMatricula(1);
        ana.setAlumnoApoderado(vinculo);
        ana.setGradoSeccion(gsM);
        ana.setAcceso(activo);
        when(matriculaRepository.findByGradoSeccionIdGradoSeccionAndAccesoNot(26, eliminado)).thenReturn(List.of(ana));

        when(notaRepository.findByMatriculaGradoSeccionIdGradoSeccionAndBimestreAndAccesoNot(26, (byte) 1, eliminado))
                .thenReturn(List.of(nota("B", "Necesita reforzar las operaciones.")));
        when(notaRepository.findByMatriculaGradoSeccionIdGradoSeccionAndBimestreAndAccesoNot(26, (byte) 2, eliminado))
                .thenReturn(List.of(nota("A", null)));
        when(asistenciaRepository.findByFechaBetweenAndAccesoNot(any(), any(), any()))
                .thenReturn(List.of(dia("Puntual"), dia("Puntual"), dia("Tardanza"), dia("Inasistencia")));
    }

    private Nota nota(String calificacion, String conclusion) {
        Nota n = new Nota();
        n.setMatricula(ana);
        n.setCompetencia(c1);
        n.setCalificacion(calificacion);
        n.setConclusionDescriptiva(conclusion);
        return n;
    }

    private AsistenciaAlumno dia(String estado) {
        EstadoAsistencia e = new EstadoAsistencia();
        e.setNombre(estado);
        AsistenciaAlumno a = new AsistenciaAlumno();
        a.setMatricula(ana);
        a.setEstado(e);
        return a;
    }

    @Test
    void llevaLosPeriodosHastaElElegido() {
        BoletaResponse boleta = service.boletas(26, 2, 7, roles);

        assertEquals(List.of(1, 2), boleta.periodos().stream().map(PeriodoEvaluacionResponse::numero).toList());
        assertEquals("1ro", boleta.seccion().grado());
    }

    @Test
    void juntaLasNotasDeCadaPeriodoPorCompetencia() {
        BoletaResponse.AlumnoBoleta fila = service.boletas(26, 2, 7, roles).alumnos().get(0);

        assertEquals("B", fila.notas().get(11).get(1));
        assertEquals("A", fila.notas().get(11).get(2));
        assertEquals("ALVA RUIZ, ANA", fila.nombre());
    }

    @Test
    void lasConclusionesSonSoloDelPeriodoElegido() {
        assertTrue(service.boletas(26, 2, 7, roles).alumnos().get(0).conclusiones().isEmpty());
    }

    @Test
    void resumeLaAsistenciaDelPeriodo() {
        BoletaResponse.Asistencia asistencia = service.boletas(26, 2, 7, roles).alumnos().get(0).asistencia();

        assertEquals(2, asistencia.puntual());
        assertEquals(1, asistencia.tardanza());
        assertEquals(0, asistencia.justificada());
        assertEquals(1, asistencia.inasistencia());
    }

    @Test
    void unaSeccionAjenaNoSeImprime() {
        when(notaFinalService.puedeVerSeccion(any(), any(), any())).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.boletas(26, 2, 7, roles));
    }

    @Test
    void unPeriodoInexistenteSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> service.boletas(26, 4, 7, roles));
    }
}
