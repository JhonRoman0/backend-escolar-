package com.example.Escolar.Service;

import com.example.Escolar.Dto.AsignacionRequest;
import com.example.Escolar.Dto.HorarioRequest;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.Asignacion;
import com.example.Escolar.Model.Aula;
import com.example.Escolar.Model.Curso;
import com.example.Escolar.Model.Docente;
import com.example.Escolar.Model.DocenteNivel;
import com.example.Escolar.Model.Grado;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.HorarioClase;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Model.Recreo;
import com.example.Escolar.Model.Seccion;
import com.example.Escolar.Model.TipoCurso;
import com.example.Escolar.Model.Turno;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.AulaRepository;
import com.example.Escolar.Repository.CursoRepository;
import com.example.Escolar.Repository.DocenteNivelRepository;
import com.example.Escolar.Repository.DocenteRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.HorarioClaseRepository;
import com.example.Escolar.Repository.RecreoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AsignacionServiceTest {

    @Mock
    private AsignacionRepository asignacionRepository;
    @Mock
    private HorarioClaseRepository horarioClaseRepository;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private DocenteRepository docenteRepository;
    @Mock
    private GradoSeccionRepository gradoSeccionRepository;
    @Mock
    private AnioEscolarRepository anioEscolarRepository;
    @Mock
    private AulaRepository aulaRepository;
    @Mock
    private AccesoRepository accesoRepository;
    @Mock
    private AccesoContextoService accesoContextoService;
    @Mock
    private DocenteNivelRepository docenteNivelRepository;
    @Mock
    private RecreoRepository recreoRepository;
    @InjectMocks
    private AsignacionService asignacionService;

    private Acceso activo;
    private Acceso inactivo;
    private AnioEscolar anio;
    private Nivel primaria;
    private GradoSeccion tercerA;
    private Curso matematica;
    private Docente docente;

    @BeforeEach
    void setUp() {
        activo = acceso(AccesoConstants.ACTIVO);
        inactivo = acceso(AccesoConstants.INACTIVO);
        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(activo));
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(acceso(AccesoConstants.ELIMINADO)));

        anio = anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR);
        primaria = new Nivel();
        primaria.setIdNivel(2);
        primaria.setNombre("Primaria");

        Turno manana = new Turno();
        manana.setIdTurno(1);
        manana.setNombre("Mañana");
        manana.setHoraEntrada(LocalTime.of(7, 0));
        manana.setHoraSalida(LocalTime.of(12, 30));
        Grado tercero = new Grado();
        tercero.setIdGrado(30);
        tercero.setNombre("3.° grado");
        tercero.setNivel(primaria);
        Seccion a = new Seccion();
        a.setNombre("A");
        tercerA = new GradoSeccion();
        tercerA.setIdGradoSeccion(5);
        tercerA.setGrado(tercero);
        tercerA.setSeccion(a);
        tercerA.setTurno(manana);
        tercerA.setAnioEscolar(anio);
        when(gradoSeccionRepository.findByIdGradoSeccionAndAccesoNot(eq(5), any())).thenReturn(Optional.of(tercerA));

        matematica = curso(40, "Matemática", TipoCurso.AREA);

        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setApellidoPat("Ruiz");
        docente = new Docente();
        docente.setIdDocente(7);
        docente.setUsuario(usuario);
        docente.setAcceso(activo);
        when(docenteRepository.findByIdDocenteAndAccesoNot(eq(7), any())).thenReturn(Optional.of(docente));
        DocenteNivel enPrimaria = new DocenteNivel();
        enPrimaria.setDocente(docente);
        enPrimaria.setNivel(primaria);
        when(docenteNivelRepository.findByDocenteAndAccesoNot(eq(docente), any())).thenReturn(List.of(enPrimaria));

        aula(1, "Aula 101", false);
        aula(2, "Patio", true);

        // 3.° grado tiene recreo el lunes de 10:00 a 10:15.
        Recreo recreo = new Recreo();
        recreo.setDiaSemana((byte) 1);
        recreo.setHoraInicio(LocalTime.of(10, 0));
        recreo.setHoraFin(LocalTime.of(10, 15));
        when(recreoRepository.findByAnioEscolarIdAnioAndTurnoIdTurnoAndGradoIdGradoAndAccesoNot(
                eq(2), eq(1), eq(30), any())).thenReturn(List.of(recreo));

        when(asignacionRepository.save(any(Asignacion.class))).thenAnswer(inv -> {
            Asignacion asignacion = inv.getArgument(0);
            if (asignacion.getIdAsignacion() == null) {
                asignacion.setIdAsignacion(100);
            }
            return asignacion;
        });
    }

    private Acceso acceso(Long id) {
        Acceso a = new Acceso();
        a.setIdAcceso(id);
        return a;
    }

    private AnioEscolar anio(int id, String valor, byte estado) {
        AnioEscolar a = new AnioEscolar();
        a.setIdAnio(id);
        a.setAnio(valor);
        a.setEstado(estado);
        when(anioEscolarRepository.findByIdAnioAndAccesoNot(eq(id), any())).thenReturn(Optional.of(a));
        return a;
    }

    private Curso curso(int id, String nombre, TipoCurso tipo) {
        Curso c = new Curso();
        c.setIdCurso(id);
        c.setNombre(nombre);
        c.setTipo(tipo);
        c.setAnioEscolar(anio);
        c.setNivel(primaria);
        c.setAcceso(activo);
        when(cursoRepository.findByIdCursoAndAccesoNot(eq(id), any())).thenReturn(Optional.of(c));
        return c;
    }

    private void aula(int id, String nombre, boolean compartida) {
        Aula a = new Aula();
        a.setIdAula(id);
        a.setNombre(nombre);
        a.setUsoCompartido(compartida);
        when(aulaRepository.findByIdAulaAndAccesoNot(eq(id), any())).thenReturn(Optional.of(a));
    }

    private HorarioRequest horario(int idAula, int dia, String inicio, String fin) {
        HorarioRequest h = new HorarioRequest();
        h.setIdAula(idAula);
        h.setDiaSemana((byte) dia);
        h.setHoraInicio(LocalTime.parse(inicio));
        h.setHoraFin(LocalTime.parse(fin));
        return h;
    }

    private AsignacionRequest request(int idCurso, HorarioRequest... horarios) {
        AsignacionRequest r = new AsignacionRequest();
        r.setIdAnio(2);
        r.setIdGradoSeccion(5);
        r.setIdCurso(idCurso);
        r.setIdDocente(7);
        r.setHorarios(List.of(horarios));
        return r;
    }

    /** Una clase ya guardada de otra asignacion, de otra seccion salvo que se indique. */
    private HorarioClase claseExistente(String curso, int dia, String inicio, String fin) {
        Curso c = new Curso();
        c.setNombre(curso);
        Asignacion otra = new Asignacion();
        otra.setIdAsignacion(55);
        otra.setCurso(c);
        otra.setGradoSeccion(tercerA);
        otra.setAcceso(activo);
        HorarioClase h = new HorarioClase();
        h.setAsignacion(otra);
        h.setDiaSemana((byte) dia);
        h.setHoraInicio(LocalTime.parse(inicio));
        h.setHoraFin(LocalTime.parse(fin));
        return h;
    }

    private String error(AsignacionRequest request) {
        return assertThrows(IllegalArgumentException.class, () -> asignacionService.create(request)).getMessage();
    }

    // ── Horarios ───────────────────────────────────────────────────────────

    @Test
    void creaUnaAsignacionConUnHorarioValido() {
        assertDoesNotThrow(() -> asignacionService.create(request(40, horario(1, 1, "08:00", "09:30"))));
        verify(asignacionRepository).save(any(Asignacion.class));
    }

    @Test
    void exigeRecreoElDiaDeLaClase() {
        assertTrue(error(request(40, horario(1, 2, "08:00", "09:00")))
                .contains("Primero configura el recreo de 3.° grado – turno Mañana para el martes"));
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    void rechazaUnaClaseEncimaDelRecreo() {
        assertTrue(error(request(40, horario(1, 1, "09:30", "10:30"))).contains("choca con el recreo del lunes"));
    }

    @Test
    void rechazaUnaClaseFueraDelTurno() {
        assertTrue(error(request(40, horario(1, 1, "12:00", "13:00"))).contains("fuera del turno Mañana"));
    }

    @Test
    void rechazaQueLaSeccionTengaDosClasesALaVez() {
        when(horarioClaseRepository.findByAsignacionGradoSeccionIdGradoSeccionInAndAccesoNot(anyCollection(), any()))
                .thenReturn(List.of(claseExistente("Comunicación", 1, "08:00", "09:00")));

        assertTrue(error(request(40, horario(1, 1, "08:30", "09:30")))
                .contains("3.° grado A ya tiene Comunicación el lunes de 08:00–09:00"));
    }

    @Test
    void elCruceDelDocenteSeBuscaSoloEnElMismoAnio() {
        when(horarioClaseRepository.findByAsignacionDocenteIdDocenteAndAsignacionAnioEscolarIdAnioAndAccesoNot(
                eq(7), eq(2), any())).thenReturn(List.of(claseExistente("Arte", 1, "08:00", "09:00")));
        assertTrue(error(request(40, horario(1, 1, "08:30", "09:30"))).contains("El docente ya tiene Arte"));

        // Una clase de otro año no choca: la consulta va filtrada por el año 2.
        when(horarioClaseRepository.findByAsignacionDocenteIdDocenteAndAsignacionAnioEscolarIdAnioAndAccesoNot(
                eq(7), eq(2), any())).thenReturn(List.of());
        when(horarioClaseRepository.findByAsignacionDocenteIdDocenteAndAsignacionAnioEscolarIdAnioAndAccesoNot(
                eq(7), eq(1), any())).thenReturn(List.of(claseExistente("Arte", 1, "08:00", "09:00")));
        assertDoesNotThrow(() -> asignacionService.create(request(40, horario(1, 1, "08:30", "09:30"))));
    }

    @Test
    void unAulaNormalNoSeComparteYElPatioSi() {
        when(horarioClaseRepository.findByAulaIdAulaAndAsignacionAnioEscolarIdAnioAndAccesoNot(anyInt(), eq(2), any()))
                .thenReturn(List.of(claseExistente("Educación Física", 1, "08:00", "09:00")));

        assertTrue(error(request(40, horario(1, 1, "08:00", "09:00"))).contains("El aula Aula 101 ya está ocupada"));
        assertDoesNotThrow(() -> asignacionService.create(request(40, horario(2, 1, "08:00", "09:00"))));
    }

    @Test
    void rechazaHorariosDeLaMismaAsignacionQueSeCruzan() {
        assertTrue(error(request(40, horario(1, 1, "08:00", "09:00"), horario(2, 1, "08:30", "09:30")))
                .contains("se cruzan"));
    }

    // ── Datos de la asignacion ─────────────────────────────────────────────

    @Test
    void rechazaUnCursoDeOtroAnio() {
        matematica.setAnioEscolar(anio(1, "2026", AnioEscolarService.ESTADO_VIGENTE));
        assertTrue(error(request(40, horario(1, 1, "08:00", "09:00"))).contains("no es de la malla de Primaria"));
    }

    @Test
    void noSeAsignaUnaTransversalNiUnAreaConSubcursos() {
        curso(41, "TIC", TipoCurso.TRANSVERSAL);
        assertTrue(error(request(41, horario(1, 1, "08:00", "09:00"))).contains("transversal"));

        matematica.setTieneSubcursos(true);
        assertTrue(error(request(40, horario(1, 1, "08:00", "09:00"))).contains("se dicta en subcursos"));
    }

    @Test
    void unSubcursoSiSeAsigna() {
        curso(42, "Álgebra", TipoCurso.SUBCURSO);
        assertDoesNotThrow(() -> asignacionService.create(request(42, horario(1, 1, "08:00", "09:00"))));
    }

    @Test
    void rechazaUnDocenteQueNoEnsenaEnElNivel() {
        when(docenteNivelRepository.findByDocenteAndAccesoNot(eq(docente), any())).thenReturn(List.of());
        assertTrue(error(request(40, horario(1, 1, "08:00", "09:00"))).contains("no enseña en el nivel Primaria"));
    }

    @Test
    void unCursoDeUnaSeccionTieneUnSoloDocente() {
        Asignacion existente = new Asignacion();
        existente.setIdAsignacion(55);
        when(asignacionRepository.findByCursoIdCursoAndGradoSeccionIdGradoSeccionAndAccesoNot(eq(40), eq(5), any()))
                .thenReturn(List.of(existente));
        assertTrue(error(request(40, horario(1, 1, "08:00", "09:00"))).contains("ya tiene un docente asignado"));
    }

    @Test
    void rechazaUnCursoInactivo() {
        matematica.setAcceso(inactivo);
        assertTrue(error(request(40, horario(1, 1, "08:00", "09:00"))).contains("inactivo"));
    }

    // ── Estado del año ─────────────────────────────────────────────────────

    @Test
    void enUnAnioVigenteSeCreaLoQueFaltaPeroNoSeEditaNiElimina() {
        anio.setEstado(AnioEscolarService.ESTADO_VIGENTE);
        assertDoesNotThrow(() -> asignacionService.create(request(40, horario(1, 1, "08:00", "09:00"))));

        Asignacion guardada = new Asignacion();
        guardada.setIdAsignacion(100);
        guardada.setAnioEscolar(anio);
        when(asignacionRepository.findByIdAsignacionAndAccesoNot(eq(100), any())).thenReturn(Optional.of(guardada));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> asignacionService.update(100, request(40, horario(1, 1, "08:00", "09:00")))).getMessage()
                .contains("ya no se pueden editar ni eliminar"));
        assertThrows(IllegalArgumentException.class, () -> asignacionService.delete(100));
    }

    @Test
    void enUnAnioCerradoNoSeCreanAsignaciones() {
        anio.setEstado(AnioEscolarService.ESTADO_CERRADO);
        assertTrue(error(request(40, horario(1, 1, "08:00", "09:00"))).contains("ya no se pueden crear asignaciones"));
    }
}
