package com.example.Escolar.Service;

import com.example.Escolar.Dto.*;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReconocimientoCalificacionServiceTest {
    @Mock ReconocimientoCalificacionRepository reconocimientos;
    @Mock NotaEvaluacionRepository notas;
    @Mock NotaRepository finales;
    @Mock EvaluacionRepository evaluaciones;
    @Mock MatriculaRepository matriculas;
    @Mock UsuarioRepository usuarios;
    @Mock AccesoRepository accesos;
    @Mock EvaluacionService evaluacionService;
    @Mock ConfiguracionAcademicaService configuracion;
    @Mock AccesoContextoService contexto;
    @Mock BloqueoCalificaciones bloqueo;
    @InjectMocks ReconocimientoCalificacionService service;
    final Acceso activo = new Acceso(1L, "Activo"), eliminado = new Acceso(2L, "Eliminado");
    final List<String> roles = List.of("Docente");
    Evaluacion origen, destino;
    NotaEvaluacion nota;
    Matricula matricula;
    ConfiguracionAcademicaResponse config;

    @BeforeEach void preparar() {
        Nivel nivel = new Nivel(); nivel.setIdNivel(1);
        Grado grado = new Grado(); grado.setIdGrado(1); grado.setNivel(nivel); grado.setOrden(1); grado.setNombre("3 años");
        AnioEscolar anio = new AnioEscolar(); anio.setIdAnio(1); anio.setEstado(AnioEscolarService.ESTADO_VIGENTE);
        Turno turno = new Turno(); turno.setNombre("Mañana");
        GradoSeccion a = new GradoSeccion(); a.setIdGradoSeccion(1); a.setGrado(grado); a.setAnioEscolar(anio); a.setTurno(turno);
        GradoSeccion b = new GradoSeccion(); b.setIdGradoSeccion(2); b.setGrado(grado); b.setAnioEscolar(anio); b.setTurno(turno);
        Curso curso = new Curso(); curso.setIdCurso(1); curso.setNombre("Matemática");
        Competencia c = new Competencia(); c.setIdCompetencia(1); c.setOrden((byte)1); c.setAcceso(activo);
        Asignacion aa = new Asignacion(); aa.setIdAsignacion(1); aa.setCurso(curso); aa.setGradoSeccion(a); aa.setAnioEscolar(anio); aa.setAcceso(activo);
        Asignacion ab = new Asignacion(); ab.setIdAsignacion(2); ab.setCurso(curso); ab.setGradoSeccion(b); ab.setAnioEscolar(anio); ab.setAcceso(activo);
        origen = new Evaluacion(); origen.setIdEvaluacion(1); origen.setAsignacion(aa); origen.setCompetencias(Set.of(c)); origen.setPeriodo((byte)1); origen.setAcceso(activo); origen.setNombre("Examen A"); origen.setFecha(LocalDate.now());
        destino = new Evaluacion(); destino.setIdEvaluacion(2); destino.setAsignacion(ab); destino.setCompetencias(Set.of(c)); destino.setPeriodo((byte)1); destino.setAcceso(activo); destino.setNombre("Práctica B"); destino.setFecha(LocalDate.now());
        matricula = new Matricula(); matricula.setIdMatricula(10); matricula.setGradoSeccion(b); matricula.setAcceso(activo);
        nota = new NotaEvaluacion(); nota.setIdNotaEvaluacion(50); nota.setEvaluacion(origen); nota.setMatricula(matricula); nota.setCalificacion("15"); nota.setAcceso(activo);
        Usuario u = new Usuario(); u.setIdUsuario(7); u.setNombre("Docente"); u.setApellidoPat("Uno");
        when(usuarios.findById(7)).thenReturn(Optional.of(u));
        when(accesos.findById(1L)).thenReturn(Optional.of(activo)); when(accesos.findById(2L)).thenReturn(Optional.of(eliminado));
        when(notas.findById(50)).thenReturn(Optional.of(nota)); when(evaluaciones.findById(2)).thenReturn(Optional.of(destino));
        when(matriculas.findById(10)).thenReturn(Optional.of(matricula));
        when(evaluacionService.asignacionAccesible(2,7,roles)).thenReturn(ab);
        config = new ConfiguracionAcademicaResponse(); config.setEscala(EscalaCalificacion.VIGESIMAL);
        when(configuracion.obtener(1,1)).thenReturn(config);
        PeriodoEvaluacion periodo = new PeriodoEvaluacion(); periodo.setNumero((byte)1); periodo.setFechaInicio(LocalDate.now().minusDays(1)); periodo.setFechaFin(LocalDate.now().plusDays(1));
        when(configuracion.periodos(anio,nivel)).thenReturn(List.of(periodo));
        when(notas.saveAndFlush(any())).thenAnswer(i -> { NotaEvaluacion n = i.getArgument(0); n.setIdNotaEvaluacion(51); return n; });
        when(reconocimientos.saveAndFlush(any())).thenAnswer(i -> { ReconocimientoCalificacion r = i.getArgument(0); r.setIdReconocimiento(1); return r; });
    }
    private ReconocimientoCalificacionRequest request() { return new ReconocimientoCalificacionRequest(50,2,"15"); }
    @Test void reconoceConNombresDiferentesYConservaOriginal() {
        var r = service.reconocer(request(),7,roles);
        assertTrue(r.enSeccionActual());
        assertEquals("15",r.calificacion()); assertEquals("15",nota.getCalificacion()); assertSame(origen,nota.getEvaluacion());
        verify(notas,never()).save(nota); verify(finales,never()).save(any());
        ArgumentCaptor<ReconocimientoCalificacion> captor = ArgumentCaptor.forClass(ReconocimientoCalificacion.class);
        verify(reconocimientos).saveAndFlush(captor.capture()); assertSame(nota,captor.getValue().getNotaOrigen());
        assertEquals(10,captor.getValue().getNotaDestino().getMatricula().getIdMatricula());
    }
    @Test void nombreIgualNoEquivaleACompetenciasIguales() {
        destino.setNombre(origen.getNombre()); destino.setCompetencias(Set.of());
        assertThrows(IllegalArgumentException.class, () -> service.reconocer(request(),7,roles)); verify(notas,never()).saveAndFlush(any());
    }
    @Test void rechazaPeriodoDistinto() { destino.setPeriodo((byte)2); assertThrows(IllegalArgumentException.class, () -> service.reconocer(request(),7,roles)); }
    @Test void rechazaCursoDistinto() { Curso c = new Curso(); c.setIdCurso(99); destino.getAsignacion().setCurso(c); assertThrows(IllegalArgumentException.class, () -> service.reconocer(request(),7,roles)); }
    @Test void rechazaCalificacionInvalidaEnEscala() { config.setEscala(EscalaCalificacion.LITERAL); assertThrows(IllegalArgumentException.class, () -> service.reconocer(request(),7,roles)); verify(notas,never()).saveAndFlush(any()); }
    @Test void rechazaDuplicadoEnSeccion() {
        when(reconocimientos.existsByNotaOrigenIdNotaEvaluacionAndSeccionDestinoIdGradoSeccionAndVigencia(50,2,(byte)1)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.reconocer(request(),7,roles)); verify(notas,never()).saveAndFlush(any());
    }
    @Test void noSobrescribeDestino() {
        NotaEvaluacion existente = new NotaEvaluacion(); existente.setMatricula(matricula);
        when(notas.findByEvaluacionIdEvaluacionAndAccesoNot(2,eliminado)).thenReturn(List.of(existente));
        assertThrows(IllegalArgumentException.class, () -> service.reconocer(request(),7,roles)); verify(notas,never()).saveAndFlush(any());
    }
    @Test void finalBloqueaReconocimiento() {
        Nota fin = new Nota(); fin.setBimestre((byte)1); fin.setCompetencia(origen.getCompetencias().iterator().next());
        when(finales.findByMatriculaIdMatriculaAndAccesoNot(10,eliminado)).thenReturn(List.of(fin));
        assertThrows(IllegalArgumentException.class, () -> service.reconocer(request(),7,roles)); verify(notas,never()).saveAndFlush(any());
    }
    @Test void periodoCerradoSoloConsulta() {
        when(configuracion.periodos(any(),any())).thenReturn(List.of());
        assertThrows(IllegalArgumentException.class, () -> service.reconocer(request(),7,roles));
        when(notas.findByMatriculaIdMatriculaAndAccesoNot(10,eliminado)).thenReturn(List.of(nota));
        assertEquals(1,service.historial(10,2,7,roles).size());
    }
    @Test void historialSinEvaluacionesEnDestino() {
        when(notas.findByMatriculaIdMatriculaAndAccesoNot(10,eliminado)).thenReturn(List.of(nota));
        assertEquals("15",service.historial(10,2,7,roles).get(0).calificacion());
        verifyNoInteractions(evaluaciones);
    }
    @Test void origenEliminadoEsConsultableSinReconocer() {
        origen.setAcceso(eliminado); when(notas.findByMatriculaIdMatriculaAndAccesoNot(10,eliminado)).thenReturn(List.of(nota));
        assertNotNull(service.historial(10,2,7,roles).get(0).bloqueo());
        assertThrows(IllegalArgumentException.class, () -> service.reconocer(request(),7,roles));
    }
    @Test void retornoNoDuplicaOriginal() {
        destino.getAsignacion().setGradoSeccion(origen.getAsignacion().getGradoSeccion()); matricula.setGradoSeccion(origen.getAsignacion().getGradoSeccion());
        assertThrows(IllegalArgumentException.class, () -> service.reconocer(request(),7,roles));
    }
    @Test void reconocimientoNoPuedeSerNuevoOrigen() {
        when(reconocimientos.existsByNotaDestinoIdNotaEvaluacion(50)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.reconocer(request(),7,roles));
    }
    @Test void rechazaValorDeOrigenDesactualizado() {
        assertThrows(IllegalArgumentException.class, () -> service.reconocer(new ReconocimientoCalificacionRequest(50,2,"14"),7,roles));
        verify(notas,never()).saveAndFlush(any());
    }
    private ReconocimientoCalificacion reconocido() {
        NotaEvaluacion copia = new NotaEvaluacion(); copia.setIdNotaEvaluacion(51); copia.setMatricula(matricula);
        copia.setEvaluacion(destino); copia.setCalificacion("15"); copia.setAcceso(activo);
        ReconocimientoCalificacion r = new ReconocimientoCalificacion(); r.setIdReconocimiento(1);
        r.setNotaOrigen(nota); r.setNotaDestino(copia); r.setSeccionDestino(matricula.getGradoSeccion()); r.setVigencia((byte)1);
        when(reconocimientos.findById(1)).thenReturn(Optional.of(r)); return r;
    }
    @Test void historialDistingueReconocimientoDeOtraSeccion() {
        var r = reconocido(); r.setCalificacion("15"); r.setUsuarioRegistro(usuarios.findById(7).orElseThrow());
        GradoSeccion tercera = new GradoSeccion(); tercera.setIdGradoSeccion(3);
        tercera.setGrado(matricula.getGradoSeccion().getGrado()); tercera.setTurno(matricula.getGradoSeccion().getTurno());
        tercera.setAnioEscolar(matricula.getGradoSeccion().getAnioEscolar());
        matricula.setGradoSeccion(tercera); destino.getAsignacion().setGradoSeccion(tercera);
        when(reconocimientos.findByNotaOrigenMatriculaIdMatricula(10)).thenReturn(List.of(r));
        when(notas.findByMatriculaIdMatriculaAndAccesoNot(10,eliminado)).thenReturn(List.of(nota));
        assertFalse(service.historial(10,2,7,roles).get(0).reconocimientos().get(0).enSeccionActual());
    }
    @Test void anulaConAuditoriaSinTocarOriginal() {
        ReconocimientoCalificacion r = reconocido(); service.anular(1,"Destino incorrecto",7,roles);
        assertNull(r.getVigencia()); assertNotNull(r.getFechaAnulacion()); assertEquals("Destino incorrecto", r.getMotivoAnulacion());
        assertEquals(eliminado,r.getNotaDestino().getAcceso()); assertEquals(activo,nota.getAcceso()); assertEquals("15",nota.getCalificacion());
        verify(finales,never()).save(any());
    }
    @Test void finalBloqueaAnulacion() {
        ReconocimientoCalificacion r = reconocido(); Nota fin = new Nota(); fin.setBimestre((byte)1); fin.setCompetencia(origen.getCompetencias().iterator().next());
        when(finales.findByMatriculaIdMatriculaAndAccesoNot(10,eliminado)).thenReturn(List.of(fin));
        assertThrows(IllegalArgumentException.class, () -> service.anular(1,"Corrección",7,roles)); assertNotNull(r.getVigencia());
        verify(notas,never()).save(any());
    }
    @Test void permiteMismoOriginalEnTerceraSeccion() {
        GradoSeccion tercera = new GradoSeccion(); tercera.setIdGradoSeccion(3); tercera.setGrado(matricula.getGradoSeccion().getGrado());
        tercera.setTurno(matricula.getGradoSeccion().getTurno()); tercera.setAnioEscolar(matricula.getGradoSeccion().getAnioEscolar());
        destino.getAsignacion().setGradoSeccion(tercera); matricula.setGradoSeccion(tercera);
        assertEquals("ACTIVO", service.reconocer(request(),7,roles).estado());
        verify(reconocimientos).existsByNotaOrigenIdNotaEvaluacionAndSeccionDestinoIdGradoSeccionAndVigencia(50,3,(byte)1);
    }
    @Test void falloPersistenciaPropagaErrorSinGuardarFinales() {
        doThrow(new org.springframework.dao.DataIntegrityViolationException("duplicado")).when(reconocimientos).saveAndFlush(any());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> service.reconocer(request(),7,roles));
        verify(finales,never()).save(any());
        // El rollback real requiere la prueba de integracion con MySQL; un mock no lo demuestra.
    }
    @Test void interceptorTransaccionalRevierteAnteFalloDeIntegridad() {
        doThrow(new org.springframework.dao.DataIntegrityViolationException("duplicado")).when(reconocimientos).saveAndFlush(any());
        var manager = mock(org.springframework.transaction.PlatformTransactionManager.class);
        var status = new org.springframework.transaction.support.SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(status);
        var factory = new org.springframework.aop.framework.ProxyFactory(service);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(manager,
            new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        var proxy = (ReconocimientoCalificacionService) factory.getProxy();
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> proxy.reconocer(request(),7,roles));
        verify(manager).rollback(status); verify(manager,never()).commit(any());
    }
}
