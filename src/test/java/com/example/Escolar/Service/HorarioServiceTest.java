package com.example.Escolar.Service;

import com.example.Escolar.Dto.MiHorarioResponse;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.HorarioClaseRepository;
import com.example.Escolar.Repository.MatriculaRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HorarioServiceTest {
    @Mock HorarioClaseRepository horarioClaseRepository;
    @Mock AnioEscolarRepository anioEscolarRepository;
    @Mock MatriculaRepository matriculaRepository;
    @Mock AccesoRepository accesoRepository;
    @Mock AccesoContextoService accesoContextoService;
    @Mock RecreoRepository recreoRepository;
    @Mock GradoSeccionRepository gradoSeccionRepository;
    @InjectMocks HorarioService service;

    private final Acceso eliminado = new Acceso(AccesoConstants.ELIMINADO, "Eliminado");
    private final Turno manana = turno(1, "Mañana");
    private final Turno tarde = turno(2, "Tarde");
    private final Nivel primaria = nivel(2, "Primaria");
    private final Grado tercero = grado(30, "3ro", primaria);
    private final Grado quinto = grado(50, "5to", primaria);
    private Docente docente;

    @BeforeEach
    void preparar() {
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        Usuario usuario = new Usuario();
        usuario.setNombre("ANA");
        usuario.setApellidoPat("DIAZ");
        usuario.setApellidoMat("RUIZ");
        docente = new Docente();
        docente.setIdDocente(5);
        docente.setUsuario(usuario);
        AnioEscolar vigente = new AnioEscolar();
        vigente.setIdAnio(2);
        when(anioEscolarRepository.findByEstadoAndAccesoNot(HorarioService.ESTADO_ACTIVO, eliminado)).thenReturn(Optional.of(vigente));
        when(accesoContextoService.docenteDeUsuario(7)).thenReturn(Optional.of(docente));
        when(recreoRepository.findByAnioEscolarIdAnioAndTurnoIdTurnoAndGradoIdGradoAndAccesoNot(any(), any(), any(), eq(eliminado)))
                .thenAnswer(i -> List.of(recreo(i.getArgument(1), i.getArgument(2))));
    }

    private static Turno turno(int id, String nombre) {
        Turno t = new Turno();
        t.setIdTurno(id);
        t.setNombre(nombre);
        return t;
    }

    private static Nivel nivel(int id, String nombre) {
        Nivel n = new Nivel();
        n.setIdNivel(id);
        n.setNombre(nombre);
        return n;
    }

    private static Grado grado(int id, String nombre, Nivel nivel) {
        Grado g = new Grado();
        g.setIdGrado(id);
        g.setNombre(nombre);
        g.setNivel(nivel);
        return g;
    }

    /** Un recreo del grado y turno pedidos. */
    private Recreo recreo(Integer idTurno, Integer idGrado) {
        Recreo r = new Recreo();
        r.setTurno(idTurno == 1 ? manana : tarde);
        r.setGrado(idGrado == 30 ? tercero : quinto);
        r.setDiaSemana((byte) 1);
        r.setHoraInicio(LocalTime.of(10, 0));
        r.setHoraFin(LocalTime.of(10, 30));
        return r;
    }

    private HorarioClase clase(String curso, Grado grado, String seccion, Turno turno) {
        Seccion s = new Seccion();
        s.setNombre(seccion);
        GradoSeccion gs = new GradoSeccion();
        gs.setIdGradoSeccion(grado.getIdGrado() * 10 + seccion.charAt(0));
        gs.setGrado(grado);
        gs.setSeccion(s);
        gs.setTurno(turno);
        Curso c = new Curso();
        c.setNombre(curso);
        AnioEscolar anio = new AnioEscolar();
        anio.setIdAnio(2);
        Asignacion a = new Asignacion();
        a.setCurso(c);
        a.setDocente(docente);
        a.setGradoSeccion(gs);
        a.setAnioEscolar(anio);
        Aula aula = new Aula();
        aula.setIdAula(1);
        aula.setNombre("A-101");
        HorarioClase h = new HorarioClase();
        h.setAsignacion(a);
        h.setAula(aula);
        h.setDiaSemana((byte) 1);
        h.setHoraInicio(LocalTime.of(8, 0));
        h.setHoraFin(LocalTime.of(9, 30));
        return h;
    }

    private void dicta(HorarioClase... clases) {
        when(horarioClaseRepository.findPlanosPorDocenteYAnio(eliminado, 2, 5)).thenReturn(List.of(clases));
    }

    @Test
    void miHorarioTraeTodasLasClasesConSuNivelYTurno() {
        dicta(clase("Matemática", tercero, "A", manana), clase("Álgebra", quinto, "B", tarde));

        MiHorarioResponse horario = service.mio(null, 7);

        assertEquals(2, horario.clases().size());
        assertEquals("Primaria", horario.clases().get(0).getNivel());
        assertEquals("Tarde", horario.clases().get(1).getTurno());
    }

    @Test
    void miHorarioUsaElAnioElegido() {
        service.mio(9, 7);

        verify(horarioClaseRepository).findPlanosPorDocenteYAnio(eliminado, 9, 5);
    }

    @Test
    void conUnSoloGradoEnElTurnoMuestraSuRecreo() {
        dicta(clase("Matemática", tercero, "A", manana), clase("Ciencia", tercero, "A", manana));

        var recreos = service.mio(null, 7).recreos();

        assertEquals(1, recreos.size());
        assertEquals("3ro", recreos.get(0).grado());
    }

    @Test
    void elMismoGradoEnVariasSeccionesTambienMuestraElRecreo() {
        dicta(clase("Matemática", tercero, "A", manana), clase("Matemática", tercero, "B", manana));

        assertEquals(1, service.mio(null, 7).recreos().size());
    }

    @Test
    void conVariosGradosEnElTurnoNoMuestraRecreo() {
        dicta(clase("Matemática", tercero, "A", manana), clase("Matemática", quinto, "A", manana));

        assertTrue(service.mio(null, 7).recreos().isEmpty());
    }

    @Test
    void cadaTurnoDecideSuRecreo() {
        dicta(clase("Matemática", tercero, "A", manana),
                clase("Álgebra", tercero, "A", tarde), clase("Álgebra", quinto, "A", tarde));

        var recreos = service.mio(null, 7).recreos();

        assertEquals(1, recreos.size());
        assertEquals("Mañana", recreos.get(0).turno());
    }

    @Test
    void sinDocenteVinculadoAvisa() {
        when(accesoContextoService.docenteDeUsuario(7)).thenReturn(Optional.empty());

        var ex = assertThrows(IllegalArgumentException.class, () -> service.mio(null, 7));

        assertTrue(ex.getMessage().contains("no está vinculado a un docente"));
        verify(horarioClaseRepository, never()).findPlanosPorDocenteYAnio(any(), any(), eq(5));
    }

    @Test
    void losRecreosDeUnaSeccionSonLosDeSuGradoYTurno() {
        GradoSeccion gs = clase("Matemática", quinto, "A", tarde).getAsignacion().getGradoSeccion();
        AnioEscolar anio = new AnioEscolar();
        anio.setIdAnio(2);
        gs.setAnioEscolar(anio);
        when(gradoSeccionRepository.findById(9)).thenReturn(Optional.of(gs));

        var recreos = service.recreosDeSeccion(9);

        assertEquals("5to", recreos.get(0).grado());
        assertEquals("Tarde", recreos.get(0).turno());
    }

    @Test
    void sinVerColegioUnDocenteNoConsultaElHorarioDeCualquierAlumno() {
        Matricula m = new Matricula();
        m.setGradoSeccion(clase("Matemática", tercero, "A", manana).getAsignacion().getGradoSeccion());
        m.getGradoSeccion().setAnioEscolar(new AnioEscolar());
        m.getGradoSeccion().getAnioEscolar().setIdAnio(2);
        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(new Acceso(AccesoConstants.ACTIVO, "Activo")));
        when(matriculaRepository.findActivasPorAlumnoId(eq(4), any())).thenReturn(List.of(m));
        when(accesoContextoService.alcance("HORARIOS", 7, List.of("Docente"))).thenReturn(AccesoContextoService.Alcance.DOCENTE);

        assertThrows(com.example.Escolar.Exception.ResourceNotFoundException.class,
                () -> service.porAlumno(4, null, 7, List.of("Docente")));

        when(accesoContextoService.tieneAccion("HORARIOS", "VER_COLEGIO", 7, List.of("Docente"))).thenReturn(true);
        assertDoesNotThrow(() -> service.porAlumno(4, null, 7, List.of("Docente")));
    }
}
