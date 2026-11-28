package com.example.Escolar.Service;

import com.example.Escolar.Dto.ConfiguracionAcademicaResponse;
import com.example.Escolar.Dto.PeriodoEvaluacionResponse;
import com.example.Escolar.Dto.PortalApoderadoDtos;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalApoderadoServiceTest {
    @Mock AccesoContextoService accesoContextoService;
    @Mock AlumnoApoderadoRepository alumnoApoderadoRepository;
    @Mock MatriculaRepository matriculaRepository;
    @Mock AccesoRepository accesoRepository;
    @Mock AsistenciaService asistenciaService;
    @Mock HorarioService horarioService;
    @Mock MatriculaService matriculaService;
    @Mock MallaEvaluable mallaEvaluable;
    @Mock ConfiguracionAcademicaService configuracionService;
    @Mock EvaluacionRepository evaluacionRepository;
    @Mock NotaEvaluacionRepository notaEvaluacionRepository;
    @Mock PagoMatriculaRepository pagoMatriculaRepository;
    @Mock ReconocimientoCalificacionRepository reconocimientoRepository;
    @InjectMocks PortalApoderadoService service;

    private final Acceso activo = new Acceso(AccesoConstants.ACTIVO, "Activo");
    private final Acceso eliminado = new Acceso(AccesoConstants.ELIMINADO, "Eliminado");
    private Matricula actual;
    private Matricula anterior;
    private Alumno alumno;
    private Curso matematica;
    private final Integer idRolActivo = 1;

    private Matricula matricula(int id, int idAnio, String anioTexto, byte estado) {
        AnioEscolar anio = new AnioEscolar();
        anio.setIdAnio(idAnio);
        anio.setAnio(anioTexto);
        anio.setEstado(estado);
        Nivel nivel = new Nivel();
        nivel.setIdNivel(2);
        nivel.setNombre("Primaria");
        Grado grado = new Grado();
        grado.setNombre("3ro");
        grado.setNivel(nivel);
        GradoSeccion gs = new GradoSeccion();
        gs.setIdGradoSeccion(30 + id);
        gs.setGrado(grado);
        gs.setAnioEscolar(anio);
        AlumnoApoderado vinculo = new AlumnoApoderado();
        vinculo.setAlumno(alumno);
        Matricula m = new Matricula();
        m.setIdMatricula(id);
        m.setGradoSeccion(gs);
        m.setAlumnoApoderado(vinculo);
        m.setAcceso(activo);
        when(matriculaRepository.findById(id)).thenReturn(Optional.of(m));
        return m;
    }

    @BeforeEach
    void preparar() {
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        alumno = new Alumno();
        alumno.setIdAlumno(9);
        alumno.setNombre("ANA");
        alumno.setApellidoPat("PEREZ");
        alumno.setApellidoMat("RUIZ");
        alumno.setAcceso(activo);
        actual = matricula(5, 2, "2026", AnioEscolarService.ESTADO_VIGENTE);
        anterior = matricula(4, 1, "2025", (byte) 3);
        // Usuario 7 es apoderado de Ana; el 8 no.
        when(accesoContextoService.esApoderadoDe(eq(7), any())).thenReturn(true);
        when(accesoContextoService.esApoderadoDe(eq(8), any())).thenReturn(false);
        when(accesoContextoService.vinculoEfectivo(7, idRolActivo))
                .thenReturn(AccesoContextoService.Alcance.APODERADO);
        when(accesoContextoService.vinculoEfectivo(8, idRolActivo))
                .thenReturn(AccesoContextoService.Alcance.NINGUNO);
        matematica = new Curso();
        matematica.setIdCurso(1);
        matematica.setNombre("Matemática");
    }

    @Test
    void otroApoderadoNoVeAlAlumno() {
        assertThrows(ResourceNotFoundException.class, () -> service.notas(5, 8, idRolActivo));
        assertThrows(ResourceNotFoundException.class, () -> service.pagos(5, 8, idRolActivo));
        assertThrows(ResourceNotFoundException.class, () -> service.asistencia(5, null, 8, idRolActivo));
        assertThrows(ResourceNotFoundException.class, () -> service.horario(5, 8, idRolActivo));
        assertThrows(ResourceNotFoundException.class, () -> service.resumen(5, 8, idRolActivo));
        assertThrows(ResourceNotFoundException.class, () -> service.recreos(5, 8, idRolActivo));
    }

    @Test
    void rolDocenteActivoNoAbreLaMatriculaDeOtroApoderado() {
        when(accesoContextoService.vinculoEfectivo(9, idRolActivo))
                .thenReturn(AccesoContextoService.Alcance.DOCENTE);

        assertThrows(ResourceNotFoundException.class, () -> service.notas(5, 9, idRolActivo));

        verify(accesoContextoService, never()).esApoderadoDe(eq(9), any());
    }

    @Test
    void listaLosHijosConTodosSusAnios() {
        Apoderado apoderado = new Apoderado();
        AlumnoApoderado vinculo = new AlumnoApoderado();
        vinculo.setAlumno(alumno);
        when(accesoContextoService.apoderadoDeUsuario(7)).thenReturn(Optional.of(apoderado));
        when(alumnoApoderadoRepository.findByApoderado(apoderado)).thenReturn(List.of(vinculo));
        when(matriculaRepository.findByAlumnoApoderadoAlumnoIdAlumnoAndAccesoNot(9, eliminado)).thenReturn(List.of(anterior, actual));

        var hijos = service.hijos(7, idRolActivo);

        assertEquals(1, hijos.size());
        assertEquals("ANA PEREZ RUIZ", hijos.get(0).nombre());
        var matriculas = hijos.get(0).matriculas();
        assertEquals(List.of("2026", "2025"), matriculas.stream().map(PortalApoderadoDtos.MatriculaHijo::anio).toList());
        assertTrue(matriculas.get(0).vigente());
        assertFalse(matriculas.get(1).vigente());
    }

    @Test
    void sinSerApoderadoNoTieneHijos() {
        when(accesoContextoService.apoderadoDeUsuario(1)).thenReturn(Optional.empty());
        assertTrue(service.hijos(1, idRolActivo).isEmpty());
    }

    @Test
    void lasNotasSeVenApenasSeRegistran() {
        Competencia resuelve = new Competencia();
        resuelve.setIdCompetencia(11);
        resuelve.setNombre("Resuelve problemas");
        when(mallaEvaluable.areas(any(), any())).thenReturn(List.of(new MallaEvaluable.Area(matematica, List.of(resuelve))));
        ConfiguracionAcademicaResponse config = new ConfiguracionAcademicaResponse();
        config.setPeriodos(List.of(new PeriodoEvaluacionResponse(1, "Primer bimestre", null, null, "ABIERTO")));
        when(configuracionService.obtener(2, 2)).thenReturn(config);
        Asignacion asignacion = new Asignacion();
        asignacion.setCurso(matematica);
        asignacion.setGradoSeccion(actual.getGradoSeccion());
        Evaluacion examen = evaluacion(100, "Examen 1", asignacion);
        Evaluacion practica = evaluacion(101, "Práctica 1", asignacion);
        when(evaluacionRepository.findByAsignacionGradoSeccionIdGradoSeccionAndAccesoNot(35, eliminado)).thenReturn(List.of(examen, practica));
        NotaEvaluacion nota = nota(1, examen, "AD");
        when(notaEvaluacionRepository.findByMatriculaIdMatriculaAndAccesoNot(5, eliminado)).thenReturn(List.of(nota));

        var cursos = service.notas(5, 7, idRolActivo);

        var periodo = cursos.get(0).periodos().get(0);
        assertEquals("Primer bimestre", periodo.nombre());
        assertEquals("AD", periodo.evaluaciones().get(0).calificacion());
        // La práctica aún no tiene nota: se muestra sin calificación.
        assertNull(periodo.evaluaciones().get(1).calificacion());
        // Las anuladas no llegan: el repositorio filtra las eliminadas.
        verify(notaEvaluacionRepository).findByMatriculaIdMatriculaAndAccesoNot(5, eliminado);
    }

    @Test
    void muestraLasNotasDeLaSeccionAnteriorSalvoLasYaUsadas() {
        when(mallaEvaluable.areas(any(), any())).thenReturn(List.of(new MallaEvaluable.Area(matematica, List.of())));
        ConfiguracionAcademicaResponse config = new ConfiguracionAcademicaResponse();
        config.setPeriodos(List.of(new PeriodoEvaluacionResponse(1, "Primer bimestre", null, null, "CERRADO")));
        when(configuracionService.obtener(2, 2)).thenReturn(config);
        // Sección anterior: 1ro B, Mañana.
        GradoSeccion anteriorSeccion = new GradoSeccion();
        anteriorSeccion.setIdGradoSeccion(99);
        anteriorSeccion.setGrado(actual.getGradoSeccion().getGrado());
        Seccion b = new Seccion();
        b.setNombre("B");
        anteriorSeccion.setSeccion(b);
        Turno manana = new Turno();
        manana.setNombre("Mañana");
        anteriorSeccion.setTurno(manana);
        Asignacion vieja = new Asignacion();
        vieja.setCurso(matematica);
        vieja.setGradoSeccion(anteriorSeccion);
        NotaEvaluacion libre = nota(7, evaluacion(110, "PC 01", vieja), "15");
        NotaEvaluacion usada = nota(8, evaluacion(111, "PC 02", vieja), "12");
        when(notaEvaluacionRepository.findByMatriculaIdMatriculaAndAccesoNot(5, eliminado)).thenReturn(List.of(libre, usada));
        ReconocimientoCalificacion reconocida = new ReconocimientoCalificacion();
        reconocida.setNotaOrigen(usada);
        reconocida.setVigencia(ReconocimientoCalificacion.VIGENTE);
        when(reconocimientoRepository.findByNotaOrigenMatriculaIdMatricula(5)).thenReturn(List.of(reconocida));

        var evaluaciones = service.notas(5, 7, idRolActivo).get(0).periodos().get(0).evaluaciones();

        assertEquals(1, evaluaciones.size());
        assertEquals("15", evaluaciones.get(0).calificacion());
        assertEquals("3ro B · Mañana", evaluaciones.get(0).otraSeccion());
    }

    private NotaEvaluacion nota(int id, Evaluacion evaluacion, String calificacion) {
        NotaEvaluacion n = new NotaEvaluacion();
        n.setIdNotaEvaluacion(id);
        n.setEvaluacion(evaluacion);
        n.setCalificacion(calificacion);
        return n;
    }

    private Evaluacion evaluacion(int id, String nombre, Asignacion asignacion) {
        Evaluacion e = new Evaluacion();
        e.setIdEvaluacion(id);
        e.setNombre(nombre);
        e.setPeriodo((byte) 1);
        e.setFecha(LocalDate.of(2026, 4, id - 90));
        e.setAsignacion(asignacion);
        e.setAcceso(activo);
        return e;
    }

    @Test
    void elSaldoDescuentaLoPagado() {
        actual.setMonto(new BigDecimal("300.00"));
        when(matriculaService.pagadoNeto(5)).thenReturn(new BigDecimal("200.00"));
        PagoMatricula pago = new PagoMatricula();
        pago.setTipo(TipoPago.PAGO);
        pago.setMonto(new BigDecimal("200.00"));
        pago.setFecha(LocalDateTime.now());
        when(pagoMatriculaRepository.findByMatriculaIdMatriculaAndAccesoNotOrderByFecha(5, eliminado)).thenReturn(List.of(pago));

        var pagos = service.pagos(5, 7, idRolActivo);

        assertEquals(new BigDecimal("100.00"), pagos.saldo());
        assertEquals(1, pagos.pagos().size());
    }

    @Test
    void laAsistenciaCuentaCadaEstadoDelMes() {
        YearMonth mes = YearMonth.of(2025, 5);
        when(asistenciaService.diasDeMatricula(anterior, mes.atDay(1), mes.atEndOfMonth())).thenReturn(List.of(
                dia(1, AsistenciaService.ESTADO_SIN_CLASES),
                dia(2, AsistenciaService.ESTADO_PUNTUAL),
                dia(5, AsistenciaService.ESTADO_TARDANZA),
                dia(6, AsistenciaService.ESTADO_INASISTENCIA)));

        var asistencia = service.asistencia(4, mes, 7, idRolActivo);

        assertEquals(1, asistencia.puntuales());
        assertEquals(1, asistencia.tardanzas());
        assertEquals(1, asistencia.faltas());
        assertEquals(4, asistencia.dias().size());
    }

    private static PortalApoderadoDtos.DiaAsistencia dia(int d, String estado) {
        return new PortalApoderadoDtos.DiaAsistencia(LocalDate.of(2025, 5, d), estado, null, null, null, null);
    }
}
