package com.example.Escolar.Service;

import com.example.Escolar.Dto.FichaMatriculaResponse;
import com.example.Escolar.Exception.ResourceNotFoundException;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.*;
import com.example.Escolar.Service.AccesoContextoService.Alcance;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FichaMatriculaTest {
    @Mock MatriculaRepository matriculaRepository;
    @Mock HistorialMatriculaRepository historialMatriculaRepository;
    @Mock AlumnoApoderadoRepository alumnoApoderadoRepository;
    @Mock GradoSeccionRepository gradoSeccionRepository;
    @Mock PagoMatriculaRepository pagoMatriculaRepository;
    @Mock AccesoRepository accesoRepository;
    @Mock AccesoContextoService accesoContextoService;
    @InjectMocks MatriculaService service;

    private final Acceso activo = new Acceso(AccesoConstants.ACTIVO, "Activo");
    private final Acceso eliminado = new Acceso(AccesoConstants.ELIMINADO, "Eliminado");
    private final List<String> roles = List.of("Secretaria");
    private Alumno alumno;
    private AlumnoApoderado vinculo;

    @BeforeEach
    void preparar() {
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        alumno = new Alumno();
        alumno.setIdAlumno(1);
        alumno.setNombre("CARLOS");
        alumno.setApellidoPat("PEREZ");
        alumno.setApellidoMat("RUIZ");
        alumno.setSexo(Sexo.MASCULINO);
        vinculo = new AlumnoApoderado();
        vinculo.setAlumno(alumno);
        vinculo.setApoderado(apoderado("ROSA"));
        vinculo.setApoPrincipal((byte) 1);
    }

    private Apoderado apoderado(String nombre) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setApellidoPat("DIAZ");
        usuario.setApellidoMat("LOPEZ");
        Apoderado apoderado = new Apoderado();
        apoderado.setUsuario(usuario);
        return apoderado;
    }

    private Matricula matricula(int id, String anioTexto, String gradoNombre) {
        AnioEscolar anio = new AnioEscolar();
        anio.setAnio(anioTexto);
        Nivel nivel = new Nivel();
        nivel.setNombre("Secundaria");
        Grado grado = new Grado();
        grado.setNombre(gradoNombre);
        grado.setNivel(nivel);
        Seccion seccion = new Seccion();
        seccion.setNombre("A");
        Turno turno = new Turno();
        turno.setNombre("Mañana");
        GradoSeccion gs = new GradoSeccion();
        gs.setAnioEscolar(anio);
        gs.setGrado(grado);
        gs.setSeccion(seccion);
        gs.setTurno(turno);
        Usuario registrador = new Usuario();
        registrador.setNombre("ADMIN");
        registrador.setApellidoPat("SISTEMA");
        Matricula m = new Matricula();
        m.setIdMatricula(id);
        m.setAlumnoApoderado(vinculo);
        m.setGradoSeccion(gs);
        m.setUsuario(registrador);
        m.setAcceso(activo);
        return m;
    }

    private FichaMatriculaResponse fichaDe2027() {
        Matricula de2026 = matricula(1, "2026", "1ro");
        Matricula de2027 = matricula(2, "2027", "2do");
        when(accesoContextoService.alcance("MATRICULAS", 7, roles)).thenReturn(Alcance.TODO);
        when(matriculaRepository.findByIdMatriculaAndAccesoNot(2, eliminado)).thenReturn(Optional.of(de2027));
        // El repositorio ya excluye las eliminadas y las devuelve sin orden.
        when(matriculaRepository.findByAlumnoApoderadoAlumnoIdAlumnoAndAccesoNot(1, eliminado))
                .thenReturn(List.of(de2027, de2026));
        when(alumnoApoderadoRepository.findByAlumno(alumno)).thenReturn(List.of(vinculo));
        return service.ficha(2, 7, roles);
    }

    @Test
    void laFichaDestacaLaMatriculaElegida() {
        FichaMatriculaResponse ficha = fichaDe2027();

        assertEquals(2, ficha.matricula().idMatricula());
        assertEquals("2do A", ficha.matricula().gradoSeccion());
    }

    @Test
    void laTrayectoriaSaleOrdenadaPorAnio() {
        assertEquals(List.of("2026", "2027"),
                fichaDe2027().trayectoria().stream().map(FichaMatriculaResponse.TrayectoriaFicha::anio).toList());
    }

    @Test
    void lasMatriculasEliminadasNoSeConsultan() {
        fichaDe2027();

        org.mockito.Mockito.verify(matriculaRepository)
                .findByAlumnoApoderadoAlumnoIdAlumnoAndAccesoNot(1, eliminado);
    }

    @Test
    void incluyeAlApoderadoYAlPrincipalPrimero() {
        AlumnoApoderado segundo = new AlumnoApoderado();
        segundo.setAlumno(alumno);
        segundo.setApoderado(apoderado("LUIS"));
        segundo.setApoPrincipal((byte) 0);
        Matricula m = matricula(2, "2027", "2do");
        when(accesoContextoService.alcance("MATRICULAS", 7, roles)).thenReturn(Alcance.TODO);
        when(matriculaRepository.findByIdMatriculaAndAccesoNot(2, eliminado)).thenReturn(Optional.of(m));
        when(matriculaRepository.findByAlumnoApoderadoAlumnoIdAlumnoAndAccesoNot(1, eliminado)).thenReturn(List.of(m));
        when(alumnoApoderadoRepository.findByAlumno(alumno)).thenReturn(List.of(segundo, vinculo));

        var apoderados = service.ficha(2, 7, roles).apoderados();

        assertEquals("ROSA DIAZ LOPEZ", apoderados.get(0).nombre());
        assertTrue(apoderados.get(0).principal());
        assertFalse(apoderados.get(1).principal());
    }

    @Test
    void unApoderadoNoVeLaFichaDeOtroAlumno() {
        Matricula ajena = matricula(9, "2027", "2do");
        Apoderado propio = new Apoderado();
        when(accesoContextoService.alcance("MATRICULAS", 7, roles)).thenReturn(Alcance.APODERADO);
        when(accesoContextoService.apoderadoDeUsuario(7)).thenReturn(Optional.of(propio));
        when(alumnoApoderadoRepository.findByApoderado(propio)).thenReturn(List.of());
        when(matriculaRepository.findByIdMatriculaAndAccesoNot(9, eliminado)).thenReturn(Optional.of(ajena));

        assertThrows(ResourceNotFoundException.class, () -> service.ficha(9, 7, roles));
    }
}
