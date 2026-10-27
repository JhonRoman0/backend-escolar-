package com.example.Escolar.Service;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Alumno;
import com.example.Escolar.Model.AlumnoApoderado;
import com.example.Escolar.Model.AnioEscolar;
import com.example.Escolar.Model.Aula;
import com.example.Escolar.Model.Grado;
import com.example.Escolar.Model.GradoSeccion;
import com.example.Escolar.Model.Matricula;
import com.example.Escolar.Model.Nivel;
import com.example.Escolar.Model.Seccion;
import com.example.Escolar.Model.Turno;
import com.example.Escolar.Repository.AccesoRepository;
import com.example.Escolar.Repository.AlumnoRepository;
import com.example.Escolar.Repository.AnioEscolarRepository;
import com.example.Escolar.Repository.GradoSeccionRepository;
import com.example.Escolar.Repository.MatriculaRepository;
import com.example.Escolar.Service.MatriculaReglasService.Regla;
import com.example.Escolar.Service.OrdenAcademicoService.Posicion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatriculaReglasServiceTest {

    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private GradoSeccionRepository gradoSeccionRepository;
    @Mock
    private AnioEscolarRepository anioEscolarRepository;
    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private AccesoRepository accesoRepository;
    @Mock
    private OrdenAcademicoService ordenAcademicoService;
    @Mock
    private PagoConfigService pagoConfigService;
    @InjectMocks
    private MatriculaReglasService reglas;

    private final Nivel inicial = nivel(1, "Inicial");
    private final Nivel primaria = nivel(2, "Primaria");
    private final Grado cincoAnios = grado(3, "5 años", inicial, 3, false);
    private final Grado primero = grado(4, "1ro", primaria, 1, false);
    private final Grado tercero = grado(6, "3ro", primaria, 3, true);
    private final Grado cuarto = grado(7, "4to", primaria, 4, true);
    private final AnioEscolar anio2026 = anio(1, "2026", AnioEscolarService.ESTADO_VIGENTE);
    private final AnioEscolar anio2027 = anio(2, "2027", AnioEscolarService.ESTADO_POR_COMENZAR);
    private Alumno alumno;

    @BeforeEach
    void setUp() {
        Acceso eliminado = new Acceso();
        eliminado.setIdAcceso(AccesoConstants.ELIMINADO);
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        alumno = new Alumno();
        alumno.setIdAlumno(9);
        alumno.setNombre("Luis");
        alumno.setApellidoPat("García");
        when(ordenAcademicoService.posicion(any())).thenAnswer(i -> {
            Grado g = i.getArgument(0);
            return new Posicion(g.getNivel().getIdNivel(), g.getOrden());
        });
        when(ordenAcademicoService.siguiente(cincoAnios)).thenReturn(Optional.of(new Posicion(2, 1)));
        when(ordenAcademicoService.siguiente(tercero)).thenReturn(Optional.of(new Posicion(2, 4)));
    }

    private static Nivel nivel(int id, String nombre) {
        Nivel nivel = new Nivel();
        nivel.setIdNivel(id);
        nivel.setNombre(nombre);
        nivel.setOrden(id);
        return nivel;
    }

    private static Grado grado(int id, String nombre, Nivel nivel, int orden, boolean repite) {
        Grado grado = new Grado();
        grado.setIdGrado(id);
        grado.setNombre(nombre);
        grado.setNivel(nivel);
        grado.setOrden(orden);
        grado.setRepite(repite);
        return grado;
    }

    private static AnioEscolar anio(int id, String anio, byte estado) {
        AnioEscolar a = new AnioEscolar();
        a.setIdAnio(id);
        a.setAnio(anio);
        a.setEstado(estado);
        return a;
    }

    private GradoSeccion seccion(int id, Grado grado, AnioEscolar anio, Integer capacidad) {
        GradoSeccion gs = new GradoSeccion();
        gs.setIdGradoSeccion(id);
        gs.setGrado(grado);
        gs.setAnioEscolar(anio);
        Seccion seccion = new Seccion();
        seccion.setNombre("A");
        gs.setSeccion(seccion);
        Turno turno = new Turno();
        turno.setIdTurno(1);
        gs.setTurno(turno);
        if (capacidad != null) {
            Aula aula = new Aula();
            aula.setCapacidad(capacidad);
            gs.setAula(aula);
        }
        return gs;
    }

    private void cursa(Grado grado, AnioEscolar anio) {
        AlumnoApoderado vinculo = new AlumnoApoderado();
        vinculo.setAlumno(alumno);
        Matricula matricula = new Matricula();
        matricula.setIdMatricula(50);
        matricula.setAlumnoApoderado(vinculo);
        matricula.setGradoSeccion(seccion(100, grado, anio, 30));
        when(matriculaRepository.findByAlumnoApoderadoAlumnoIdAlumnoAndAccesoNot(eq(9), any())).thenReturn(List.of(matricula));
    }

    @Test
    void unAlumnoNuevoPuedeIrACualquierGrado() {
        Regla regla = reglas.regla(anio2027, null, null);
        assertNull(regla.bloqueo());
        assertNull(regla.permitidas());
    }

    @Test
    void quienNoRepiteVaSoloAlSiguienteSinEsperarElCierre() {
        cursa(cincoAnios, anio2026);

        Regla regla = reglas.regla(anio2027, alumno, null);

        assertNull(regla.bloqueo());
        assertEquals(Set.of(new Posicion(2, 1)), regla.permitidas());
        assertEquals("5 años Inicial 2026", regla.ultimoGrado());
    }

    @Test
    void quienPuedeRepetirEsperaElCierreDelAnio() {
        cursa(tercero, anio2026);

        Regla regla = reglas.regla(anio2027, alumno, null);

        assertEquals("Luis García cursa 3ro Primaria en 2026: podrá matricularse para 2027 cuando se cierre el año 2026",
                regla.bloqueo());
    }

    @Test
    void conElAnioCerradoPuedeRepetirOPasarAlSiguiente() {
        anio2026.setEstado(AnioEscolarService.ESTADO_CERRADO);
        cursa(tercero, anio2026);

        Regla regla = reglas.regla(anio2027, alumno, null);

        assertEquals(Set.of(new Posicion(2, 3), new Posicion(2, 4)), regla.permitidas());
        assertEquals(new Posicion(2, 4), regla.sugerida());
    }

    @Test
    void noSeMatriculaDosVecesEnElMismoAnio() {
        cursa(primero, anio2027);

        assertEquals("Luis García ya está matriculado en 2027", reglas.regla(anio2027, alumno, null).bloqueo());
    }

    @Test
    void rechazaUnGradoQueNoLeCorresponde() {
        anio2026.setEstado(AnioEscolarService.ESTADO_CERRADO);
        cursa(tercero, anio2026);
        GradoSeccion primeroB = seccion(200, primero, anio2027, 30);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reglas.validarSeccion(primeroB, alumno, null));
        assertEquals("El alumno no puede matricularse en 1ro Primaria: solo puede ir a su mismo grado o al siguiente",
                ex.getMessage());
    }

    @Test
    void rechazaUnaSeccionSinAulaOLlena() {
        GradoSeccion sinAula = seccion(201, cuarto, anio2027, null);
        assertThrows(IllegalArgumentException.class, () -> reglas.validarSeccion(sinAula, null, null));

        GradoSeccion llena = seccion(202, cuarto, anio2027, 2);
        when(matriculaRepository.countByGradoSeccionIdGradoSeccionAndAccesoNot(eq(202), any())).thenReturn(2L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reglas.validarSeccion(llena, null, null));
        assertEquals("La sección 4to A ya no tiene cupo", ex.getMessage());

        when(matriculaRepository.countByGradoSeccionIdGradoSeccionAndAccesoNot(eq(202), any())).thenReturn(1L);
        assertDoesNotThrow(() -> reglas.validarSeccion(llena, null, null));
    }
}
