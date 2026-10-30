package com.example.Escolar.Service;

import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlumnoContextoServiceTest {
    @Mock AlumnoRepository alumnoRepository;
    @Mock AlumnoApoderadoRepository alumnoApoderadoRepository;
    @Mock MatriculaRepository matriculaRepository;
    @Mock CloudinaryService cloudinaryService;
    @Mock AccesoContextoService accesoContextoService;
    @Mock AccesoRepository accesoRepository;
    @InjectMocks AlumnoService service;

    private Alumno alumno;
    private Acceso activo;
    private final PageRequest pagina = PageRequest.of(0, 10);
    private final List<String> roles = List.of("ADMIN");

    @BeforeEach
    void preparar() {
        activo = new Acceso();
        activo.setIdAcceso(AccesoConstants.ACTIVO);
        alumno = new Alumno();
        alumno.setIdAlumno(1);
        alumno.setAcceso(activo);
        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(activo));
        when(alumnoApoderadoRepository.findByAlumno(alumno)).thenReturn(List.of());
    }

    private Matricula matricula(int anioId, byte estado, int turnoId) {
        AnioEscolar anio = new AnioEscolar();
        anio.setIdAnio(anioId);
        anio.setAnio(String.valueOf(2020 + anioId));
        anio.setEstado(estado);
        Nivel nivel = new Nivel(); nivel.setIdNivel(2); nivel.setNombre("Primaria");
        Grado grado = new Grado(); grado.setIdGrado(3); grado.setNivel(nivel); grado.setNombre("3do");
        Turno turno = new Turno(); turno.setIdTurno(turnoId); turno.setNombre("Turno " + turnoId);
        Seccion seccion = new Seccion(); seccion.setIdSeccion(anioId * 10 + turnoId); seccion.setNombre("A");
        GradoSeccion gs = new GradoSeccion();
        gs.setIdGradoSeccion(anioId * 100 + turnoId); gs.setGrado(grado); gs.setTurno(turno);
        gs.setAnioEscolar(anio); gs.setSeccion(seccion); gs.setAcceso(activo);
        AlumnoApoderado vinculo = new AlumnoApoderado();
        vinculo.setAlumno(alumno); vinculo.setApoPrincipal((byte) 1);
        Matricula m = new Matricula(); m.setAlumnoApoderado(vinculo); m.setGradoSeccion(gs); m.setAcceso(activo);
        return m;
    }

    private void gestion(Integer anio, Integer turno, List<Matricula> matriculas) {
        when(accesoContextoService.alcance("ALUMNOS", 50, roles)).thenReturn(AccesoContextoService.Alcance.TODO);
        when(alumnoRepository.findFiltrados(AccesoConstants.ACTIVO, AccesoConstants.ELIMINADO,
                null, null, null, turno, null, anio, pagina)).thenReturn(new PageImpl<>(List.of(alumno), pagina, 1));
        when(matriculaRepository.findActivasPorAlumnoIds(List.of(1), activo)).thenReturn(matriculas);
    }

    @Test void muestraElAnioHistoricoConsultadoAunqueExistaVigente() {
        gestion(6, null, List.of(matricula(7, (byte) 1, 1), matricula(6, (byte) 2, 1)));
        var resultado = service.getAll(pagina, 50, roles, null, null, null, null, null, 6);
        assertEquals(6, resultado.getContent().get(0).getIdAnio());
        assertEquals(1, resultado.getTotalElements());
    }

    @Test void muestraElAnioFuturoConsultado() {
        gestion(8, null, List.of(matricula(7, (byte) 1, 1), matricula(8, (byte) 3, 1)));
        assertEquals(8, service.getAll(pagina, 50, roles, null, null, null, null, null, 8)
                .getContent().get(0).getIdAnio());
    }

    @Test void elEnriquecimientoRespetaTambienElTurno() {
        gestion(6, 2, List.of(matricula(6, (byte) 2, 1), matricula(6, (byte) 2, 2)));
        assertEquals(2, service.getAll(pagina, 50, roles, null, null, null, 2, null, 6)
                .getContent().get(0).getIdTurno());
    }

    @Test void ignoraRelacionesEliminadasYVinculosNoPrincipales() {
        Matricula eliminada = matricula(6, (byte) 2, 1);
        Acceso eliminado = new Acceso(); eliminado.setIdAcceso(AccesoConstants.ELIMINADO);
        eliminada.getGradoSeccion().setAcceso(eliminado);
        Matricula secundaria = matricula(6, (byte) 2, 1);
        secundaria.getAlumnoApoderado().setApoPrincipal((byte) 0);
        gestion(6, null, List.of(eliminada, secundaria, matricula(6, (byte) 2, 2)));
        assertEquals(2, service.getAll(pagina, 50, roles, null, null, null, null, null, 6)
                .getContent().get(0).getIdTurno());
    }

    @Test void sinAnioConservaLaPreferenciaPorElVigente() {
        gestion(null, 1, List.of(matricula(6, (byte) 1, 1), matricula(8, (byte) 3, 1)));
        assertEquals(6, service.getAll(pagina, 50, roles, null, null, null, 1, null, null)
                .getContent().get(0).getIdAnio());
    }

    @Test void listadoSinFiltrosConservaMatriculaActualYAlumnosSinMatricula() {
        when(accesoContextoService.alcance("ALUMNOS", 50, roles)).thenReturn(AccesoContextoService.Alcance.TODO);
        Acceso eliminado = new Acceso(); eliminado.setIdAcceso(AccesoConstants.ELIMINADO);
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        when(alumnoRepository.findByAccesoNot(eliminado, pagina)).thenReturn(new PageImpl<>(List.of(alumno)));
        when(matriculaRepository.findActivasPorAlumnoIds(List.of(1), activo))
                .thenReturn(List.of(matricula(6, (byte) 1, 1), matricula(8, (byte) 3, 1))).thenReturn(List.of());
        assertEquals(6, service.getAll(pagina, 50, roles).getContent().get(0).getIdAnio());
        assertEquals("Sin matrícula", service.getAll(pagina, 50, roles).getContent().get(0).getNivel());
    }

    @Test void apoderadoVeSoloSusAlumnosConElContextoConsultado() {
        when(accesoContextoService.alcance("ALUMNOS", 50, roles)).thenReturn(AccesoContextoService.Alcance.APODERADO);
        Apoderado apoderado = new Apoderado();
        when(accesoContextoService.apoderadoDeUsuario(50)).thenReturn(Optional.of(apoderado));
        AlumnoApoderado vinculo = new AlumnoApoderado(); vinculo.setAlumno(alumno);
        when(alumnoApoderadoRepository.findByApoderado(apoderado)).thenReturn(List.of(vinculo));
        Alumno ajeno = new Alumno(); ajeno.setIdAlumno(99);
        when(alumnoRepository.findFiltradosList(AccesoConstants.ACTIVO, AccesoConstants.ELIMINADO,
                null, null, null, null, null, 6)).thenReturn(List.of(alumno, ajeno));
        when(matriculaRepository.findActivasPorAlumnoIds(List.of(1), activo))
                .thenReturn(List.of(matricula(7, (byte) 1, 1), matricula(6, (byte) 2, 1)));
        var resultado = service.getAll(pagina, 50, roles, null, null, null, null, null, 6);
        assertEquals(1, resultado.getTotalElements());
        assertEquals(6, resultado.getContent().get(0).getIdAnio());
        verify(matriculaRepository).findActivasPorAlumnoIds(List.of(1), activo);
    }

    @Test void docenteConservaSuAlcanceAlConsultarOtroAnio() {
        when(accesoContextoService.alcance("ALUMNOS", 50, roles)).thenReturn(AccesoContextoService.Alcance.DOCENTE);
        when(accesoContextoService.gradoSeccionIdsDeDocente(50)).thenReturn(java.util.Set.of(701));
        when(accesoContextoService.matriculasDeGradoSecciones(java.util.Set.of(701)))
                .thenReturn(List.of(matricula(7, (byte) 1, 1)));
        Alumno ajeno = new Alumno(); ajeno.setIdAlumno(99);
        when(alumnoRepository.findFiltradosList(AccesoConstants.ACTIVO, AccesoConstants.ELIMINADO,
                null, null, null, null, null, 6)).thenReturn(List.of(alumno, ajeno));
        when(matriculaRepository.findActivasPorAlumnoIds(List.of(1), activo))
                .thenReturn(List.of(matricula(7, (byte) 1, 1), matricula(6, (byte) 2, 1)));
        var resultado = service.getAll(pagina, 50, roles, null, null, null, null, null, 6);
        assertEquals(1, resultado.getTotalElements());
        assertEquals(6, resultado.getContent().get(0).getIdAnio());
    }

    @Test void todosLosFiltrosSeAplicanALaMismaMatricula() {
        when(accesoContextoService.alcance("ALUMNOS", 50, roles)).thenReturn(AccesoContextoService.Alcance.TODO);
        when(alumnoRepository.findFiltrados(AccesoConstants.ACTIVO, AccesoConstants.ELIMINADO,
                2, 3, 62, 2, 602, 6, pagina)).thenReturn(new PageImpl<>(List.of(alumno)));
        Matricula nivelDistinto = matricula(6, (byte) 2, 2);
        nivelDistinto.getGradoSeccion().getGrado().getNivel().setIdNivel(99);
        Matricula gradoDistinto = matricula(6, (byte) 2, 2);
        gradoDistinto.getGradoSeccion().getGrado().setIdGrado(99);
        Matricula seccionDistinta = matricula(6, (byte) 2, 2);
        seccionDistinta.getGradoSeccion().getSeccion().setIdSeccion(99);
        Matricula relacionDistinta = matricula(6, (byte) 2, 2);
        relacionDistinta.getGradoSeccion().setIdGradoSeccion(999);
        when(matriculaRepository.findActivasPorAlumnoIds(List.of(1), activo)).thenReturn(List.of(
                nivelDistinto, gradoDistinto, seccionDistinta, relacionDistinta, matricula(6, (byte) 2, 2)));
        var respuesta = service.getAll(pagina, 50, roles, 2, 3, 62, 2, 602, 6).getContent().get(0);
        assertEquals(602, respuesta.getIdGradoSeccion());
        assertEquals(62, respuesta.getIdSeccion());
        assertEquals(2, respuesta.getIdNivel());
        assertEquals(3, respuesta.getIdGrado());
    }
}
