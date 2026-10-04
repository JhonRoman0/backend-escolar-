package com.example.Escolar.Service;

import com.example.Escolar.Dto.GradoSeccionResponse;
import com.example.Escolar.Dto.SeccionRequest;
import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.Asignacion;
import com.example.Escolar.Model.Grado;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.Matricula;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Model.Seccion;
import com.example.Escolar.Model.Turno;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.AsignacionRepository;
import com.example.Escolar.Repository.GradoRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import com.example.Escolar.Repository.SeccionRepository;
import com.example.Escolar.Repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GradoSeccionServiceTest {

    private static final Long ID_ACTIVO = 1L;
    private static final Long ID_ELIMINADO = 2L;

    @Mock
    private GradoSeccionRepository gradoSeccionRepository;
    @Mock
    private GradoRepository gradoRepository;
    @Mock
    private SeccionRepository seccionRepository;
    @Mock
    private TurnoRepository turnoRepository;
    @Mock
    private AnioEscolarRepository anioEscolarRepository;
    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private AsignacionRepository asignacionRepository;
    @Mock
    private AccesoRepository accesoRepository;
    @InjectMocks
    private GradoSeccionService service;

    private final AtomicInteger seq = new AtomicInteger(100);

    private Acceso activo;
    private Acceso eliminado;
    private Nivel nivel;
    private Turno manana;
    private Turno tarde;
    private AnioEscolar anio2026;
    private Grado grado;

    @BeforeEach
    void setUp() {
        activo = new Acceso();
        activo.setIdAcceso(ID_ACTIVO);
        eliminado = new Acceso();
        eliminado.setIdAcceso(ID_ELIMINADO);

        when(accesoRepository.findById(ID_ACTIVO)).thenReturn(Optional.of(activo));
        when(accesoRepository.findById(ID_ELIMINADO)).thenReturn(Optional.of(eliminado));

        nivel = new Nivel();
        nivel.setIdNivel(2);
        nivel.setNombre("Primaria");

        manana = new Turno();
        manana.setIdTurno(1);
        manana.setNombre("Mañana");
        tarde = new Turno();
        tarde.setIdTurno(2);
        tarde.setNombre("Tarde");

        anio2026 = new AnioEscolar();
        anio2026.setIdAnio(2026);
        anio2026.setAnio("2026");
        anio2026.setEstado((byte) 1);

        grado = new Grado();
        grado.setIdGrado(10);
        grado.setNombre("1ro");
        grado.setNivel(nivel);
        grado.setAcceso(activo);

        when(gradoRepository.findByIdGradoAndAccesoNot(anyInt(), any())).thenReturn(Optional.of(grado));
        when(turnoRepository.findByIdTurnoAndAccesoNot(anyInt(), any()))
                .thenAnswer(inv -> {
                    int id = inv.getArgument(0);
                    if (id == 1) return Optional.of(manana);
                    if (id == 2) return Optional.of(tarde);
                    return Optional.empty();
                });
        when(anioEscolarRepository.findByEstadoAndAccesoNot(any(), any())).thenReturn(Optional.of(anio2026));
        when(anioEscolarRepository.findByIdAnioAndAccesoNot(anyInt(), any())).thenReturn(Optional.of(anio2026));
        when(seccionRepository.save(any(Seccion.class))).thenAnswer(inv -> {
            Seccion s = inv.getArgument(0);
            s.setIdSeccion(seq.incrementAndGet());
            return s;
        });
        when(gradoSeccionRepository.save(any(GradoSeccion.class))).thenAnswer(inv -> {
            GradoSeccion gs = inv.getArgument(0);
            if (gs.getIdGradoSeccion() == null) gs.setIdGradoSeccion(seq.incrementAndGet());
            return gs;
        });
        when(gradoSeccionRepository.findByGradoAndAccesoNot(any(), any())).thenReturn(new ArrayList<>());
        when(matriculaRepository.findByGradoSeccionIdGradoSeccionAndAccesoNot(anyInt(), any()))
                .thenReturn(new ArrayList<>());
        when(asignacionRepository.existsByGradoSeccionIdGradoSeccionAndAccesoNot(anyInt(), any()))
                .thenReturn(false);
    }

    private SeccionRequest request(String nombre, int idTurno) {
        SeccionRequest r = new SeccionRequest();
        r.setIdGrado(10);
        r.setIdTurno(idTurno);
        r.setNombre(nombre);
        return r;
    }

    private GradoSeccion seccionExistente(String nombre, Turno turno, int idGradoSeccion) {
        Seccion s = new Seccion();
        s.setIdSeccion(seq.incrementAndGet());
        s.setNombre(nombre);
        s.setAcceso(activo);
        GradoSeccion gs = new GradoSeccion();
        gs.setIdGradoSeccion(idGradoSeccion);
        gs.setGrado(grado);
        gs.setSeccion(s);
        gs.setTurno(turno);
        gs.setAnioEscolar(anio2026);
        gs.setAcceso(activo);
        return gs;
    }

    @Test
    void creaEnElAnioVigenteCuandoNoLePidenAnio() {
        GradoSeccionResponse res = service.create(request("A", 1));
        assertEquals("A", res.getNombre());
        assertEquals(2026, res.getIdAnio());
        assertEquals("Mañana", res.getTurno());
    }

    @Test
    void normalizaElNombreConEspaciosAlrededor() {
        assertEquals("A", service.create(request("  A  ", 1)).getNombre());
    }

    @Test
    void rechazaLaSeccionRepetidaEnElMismoGradoTurnoYAnio() {
        GradoSeccion existente = seccionExistente("A", manana, 55);
        when(gradoSeccionRepository.findByGradoAndAccesoNot(any(), any()))
                .thenReturn(new ArrayList<>(List.of(existente)));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.create(request("A", 1)));
        assertTrue(e.getMessage().contains("Ya existe la sección A"));
    }

    @Test
    void aceptaLaMismaLetraEnOtroTurno() {
        GradoSeccion existente = seccionExistente("A", manana, 55);
        when(gradoSeccionRepository.findByGradoAndAccesoNot(any(), any()))
                .thenReturn(new ArrayList<>(List.of(existente)));

        assertEquals("A", service.create(request("A", 2)).getNombre());
    }

    @Test
    void borraLaSeccionYTambienLaFilaDeSeccion() {
        GradoSeccion objetivo = seccionExistente("B", manana, 55);
        when(gradoSeccionRepository.findByIdGradoSeccionAndAccesoNot(anyInt(), any()))
                .thenReturn(Optional.of(objetivo));
        when(gradoSeccionRepository.findByGradoAndAccesoNot(any(), any()))
                .thenReturn(new ArrayList<>(List.of(objetivo, seccionExistente("A", manana, 56))));

        service.delete(55);

        assertEquals(eliminado, objetivo.getAcceso());
        // Sin esto la letra queda viva para siempre y se acumulan letras huerfanas.
        assertEquals(eliminado, objetivo.getSeccion().getAcceso());
    }

    @Test
    void noBorraUnaSeccionConAlumnosMatriculados() {
        GradoSeccion objetivo = seccionExistente("A", manana, 55);
        when(gradoSeccionRepository.findByIdGradoSeccionAndAccesoNot(anyInt(), any()))
                .thenReturn(Optional.of(objetivo));
        when(gradoSeccionRepository.findByGradoAndAccesoNot(any(), any()))
                .thenReturn(new ArrayList<>(List.of(objetivo, seccionExistente("B", manana, 56))));
        when(matriculaRepository.findByGradoSeccionIdGradoSeccionAndAccesoNot(anyInt(), any()))
                .thenReturn(new ArrayList<>(List.of(new Matricula())));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.delete(55));
        assertEquals(GradoSeccionService.MENSAJE_SECCION_EN_USO, e.getMessage());
        assertEquals(activo, objetivo.getAcceso());
    }

    @Test
    void noBorraUnaSeccionConCursosAsignados() {
        GradoSeccion objetivo = seccionExistente("A", manana, 55);
        when(gradoSeccionRepository.findByIdGradoSeccionAndAccesoNot(anyInt(), any()))
                .thenReturn(Optional.of(objetivo));
        when(gradoSeccionRepository.findByGradoAndAccesoNot(any(), any()))
                .thenReturn(new ArrayList<>(List.of(objetivo, seccionExistente("B", manana, 56))));
        when(asignacionRepository.existsByGradoSeccionIdGradoSeccionAndAccesoNot(anyInt(), any()))
                .thenReturn(true);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.delete(55));
        assertEquals(GradoSeccionService.MENSAJE_SECCION_EN_USO, e.getMessage());
    }

    @Test
    void noDejaUnGradoSinNingunaSeccion() {
        GradoSeccion unica = seccionExistente("A", manana, 55);
        when(gradoSeccionRepository.findByIdGradoSeccionAndAccesoNot(anyInt(), any()))
                .thenReturn(Optional.of(unica));
        when(gradoSeccionRepository.findByGradoAndAccesoNot(any(), any()))
                .thenReturn(new ArrayList<>(List.of(unica)));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.delete(55));
        assertTrue(e.getMessage().contains("última sección"));
    }

    @Test
    void laRespuestaTraeLosContadoresDeUso() {
        GradoSeccion conAlumnos = seccionExistente("A", manana, 55);
        when(gradoSeccionRepository.findByGradoAndAccesoNot(any(), any()))
                .thenReturn(new ArrayList<>(List.of(conAlumnos)));
        when(matriculaRepository.findByGradoSeccionIdGradoSeccionAndAccesoNot(eq(55), any()))
                .thenReturn(new ArrayList<>(List.of(new Matricula())));

        List<GradoSeccionResponse> res = service.getSeccionesByGrado(10);

        assertEquals(1, res.size());
        assertTrue(res.get(0).isTieneMatriculas());
        assertFalse(res.get(0).isTieneAsignaciones());
        assertEquals(2026, res.get(0).getIdAnio());
        assertEquals("Mañana", res.get(0).getTurno());
    }

    @Test
    void elListadoOcultaLasFilasSinSeccion() {
        GradoSeccion sinLetra = new GradoSeccion();
        sinLetra.setIdGradoSeccion(57);
        sinLetra.setGrado(grado);
        sinLetra.setSeccion(null);
        sinLetra.setTurno(manana);
        sinLetra.setAnioEscolar(anio2026);
        sinLetra.setAcceso(activo);

        when(gradoSeccionRepository.findByGradoAndAccesoNot(any(), any()))
                .thenReturn(new ArrayList<>(List.of(sinLetra, seccionExistente("A", manana, 55))));

        List<GradoSeccionResponse> res = service.getSeccionesByGrado(10);

        assertEquals(1, res.size());
        assertEquals("A", res.get(0).getNombre());
    }

    @Test
    void avisaQueNoHayAnioVigenteParaCrear() {
        when(anioEscolarRepository.findByEstadoAndAccesoNot(any(), any())).thenReturn(Optional.empty());

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> service.create(request("A", 1)));
        assertTrue(e.getMessage().contains("año escolar vigente"));
    }

    @Test
    void unaAsignacionRegistradaBloqueaLaSeccion() {
        // Si el filtro por idGradoSeccion estuviera mal, ninguna asignacion
        // devolveria true y la seccion se podria borrar con cursos encima.
        when(asignacionRepository.existsByGradoSeccionIdGradoSeccionAndAccesoNot(anyInt(), any()))
                .thenReturn(true);
        assertTrue(service.tieneUso(seccionExistente("A", manana, 55)));
    }
}