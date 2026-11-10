package com.example.Escolar.Service;

import com.example.Escolar.Dto.AlumnoReporteResponse;
import com.example.Escolar.Dto.MatriculaReporteResponse;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.*;
import com.example.Escolar.Service.AccesoContextoService.Alcance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportesAlcanceTest {
    @Mock MatriculaRepository matriculaRepository;
    @Mock AlumnoRepository alumnoRepository;
    @Mock AlumnoApoderadoRepository alumnoApoderadoRepository;
    @Mock HistorialMatriculaRepository historialMatriculaRepository;
    @Mock GradoSeccionRepository gradoSeccionRepository;
    @Mock PagoMatriculaRepository pagoMatriculaRepository;
    @Mock CloudinaryService cloudinaryService;
    @Mock AccesoContextoService accesoContextoService;
    @Mock AccesoRepository accesoRepository;

    private final Acceso activo = new Acceso(AccesoConstants.ACTIVO, "Activo");
    private final Acceso eliminado = new Acceso(AccesoConstants.ELIMINADO, "Eliminado");
    private final List<String> roles = List.of("Apoderado");

    private AlumnoService alumnos() {
        return new AlumnoService(alumnoRepository, alumnoApoderadoRepository, matriculaRepository,
                cloudinaryService, accesoContextoService, accesoRepository);
    }

    private MatriculaService matriculas() {
        lenient().when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        return new MatriculaService(matriculaRepository, historialMatriculaRepository, alumnoApoderadoRepository,
                gradoSeccionRepository, pagoMatriculaRepository, accesoRepository, accesoContextoService, org.mockito.Mockito.mock(BloqueoCalificaciones.class));
    }

    private Matricula matricula(int id, int idAlumno, String apellido, String anioTexto, String gradoNombre, int ordenGrado) {
        Alumno alumno = new Alumno();
        alumno.setIdAlumno(idAlumno);
        alumno.setNombre("ANA");
        alumno.setApellidoPat(apellido);
        alumno.setApellidoMat("RUIZ");
        alumno.setAcceso(activo);
        AnioEscolar anio = new AnioEscolar();
        anio.setAnio(anioTexto);
        Nivel nivel = new Nivel();
        nivel.setNombre("Secundaria");
        nivel.setOrden(3);
        Grado grado = new Grado();
        grado.setNombre(gradoNombre);
        grado.setNivel(nivel);
        grado.setOrden(ordenGrado);
        Seccion seccion = new Seccion();
        seccion.setNombre("A");
        Turno turno = new Turno();
        turno.setNombre("Mañana");
        GradoSeccion gs = new GradoSeccion();
        gs.setAnioEscolar(anio);
        gs.setGrado(grado);
        gs.setSeccion(seccion);
        gs.setTurno(turno);
        AlumnoApoderado vinculo = new AlumnoApoderado();
        vinculo.setAlumno(alumno);
        Usuario registrador = new Usuario();
        registrador.setNombre("ADMIN");
        registrador.setApellidoPat("SISTEMA");
        Matricula m = new Matricula();
        m.setIdMatricula(id);
        m.setAlumnoApoderado(vinculo);
        m.setGradoSeccion(gs);
        m.setUsuario(registrador);
        m.setAcceso(activo);
        m.setFechaRegistro(LocalDate.of(2026, 11, 1));
        m.setMonto(new BigDecimal("300.00"));
        return m;
    }

    private void padronConMatriculas(Matricula... encontradas) {
        when(matriculaRepository.findFiltradasList(AccesoConstants.ELIMINADO, null, null, null, null, null, 2,
                AccesoConstants.ACTIVO)).thenReturn(List.of(encontradas));
    }

    @Test
    void padronMuestraLaSeccionDelAnioElegido() {
        when(accesoContextoService.alcance("ALUMNOS", 7, roles)).thenReturn(Alcance.TODO);
        padronConMatriculas(matricula(2, 1, "PEREZ", "2027", "2do", 2));

        AlumnoReporteResponse fila = alumnos().reporte(2, null, null, null, 7, roles).get(0);

        assertEquals("2do A", fila.getGradoSeccion());
        assertEquals("2027", fila.getAnio());
        assertEquals("Secundaria", fila.getNivel());
    }

    @Test
    void padronDelApoderadoSoloTraeASusHijos() {
        Apoderado apoderado = new Apoderado();
        Matricula suya = matricula(1, 1, "PEREZ", "2027", "2do", 2);
        when(accesoContextoService.alcance("ALUMNOS", 7, roles)).thenReturn(Alcance.APODERADO);
        when(accesoContextoService.apoderadoDeUsuario(7)).thenReturn(Optional.of(apoderado));
        when(alumnoApoderadoRepository.findByApoderado(apoderado)).thenReturn(List.of(suya.getAlumnoApoderado()));
        padronConMatriculas(suya, matricula(2, 9, "AJENO", "2027", "2do", 2));

        List<AlumnoReporteResponse> filas = alumnos().reporte(2, null, null, null, 7, roles);

        assertEquals(List.of(1), filas.stream().map(AlumnoReporteResponse::getIdAlumno).toList());
    }

    @Test
    void padronOcultaApoderadosSinElPermiso() {
        when(accesoContextoService.alcance("ALUMNOS", 7, roles)).thenReturn(Alcance.TODO);
        when(accesoContextoService.idUsuarioAutenticado()).thenReturn(7);
        when(accesoContextoService.rolesAutenticados()).thenReturn(roles);
        when(accesoContextoService.tienePermiso("APODERADOS", 7, roles)).thenReturn(false);
        padronConMatriculas(matricula(1, 1, "PEREZ", "2027", "2do", 2));

        assertNull(alumnos().reporte(2, null, null, null, 7, roles).get(0).getApoderados());
    }

    @Test
    void padronListaAlPrincipalPrimeroConParentescoYCelular() {
        Matricula m = matricula(1, 1, "PEREZ", "2027", "2do", 2);
        when(accesoContextoService.alcance("ALUMNOS", 7, roles)).thenReturn(Alcance.TODO);
        when(accesoContextoService.idUsuarioAutenticado()).thenReturn(7);
        when(accesoContextoService.rolesAutenticados()).thenReturn(roles);
        when(accesoContextoService.tienePermiso("APODERADOS", 7, roles)).thenReturn(true);
        when(alumnoApoderadoRepository.findByAlumno(m.getAlumnoApoderado().getAlumno())).thenReturn(List.of(
                vinculo("LUIS", (byte) 0, "Padre", null), vinculo("ROSA", (byte) 1, "Madre", "987654321")));
        padronConMatriculas(m);

        assertEquals(List.of("ROSA DIAZ LOPEZ (Madre) 987654321", "LUIS DIAZ LOPEZ (Padre)"),
                alumnos().reporte(2, null, null, null, 7, roles).get(0).getApoderados());
    }

    private AlumnoApoderado vinculo(String nombre, byte principal, String parentescoNombre, String celular) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setApellidoPat("DIAZ");
        usuario.setApellidoMat("LOPEZ");
        Apoderado apoderado = new Apoderado();
        apoderado.setUsuario(usuario);
        apoderado.setCelular(celular);
        Parentesco parentesco = new Parentesco();
        parentesco.setNombre(parentescoNombre);
        AlumnoApoderado vinculo = new AlumnoApoderado();
        vinculo.setApoderado(apoderado);
        vinculo.setApoPrincipal(principal);
        vinculo.setParentesco(parentesco);
        return vinculo;
    }

    @Test
    void matriculaCalculaSaldoMetodoYOrdenaPorGrado() {
        Matricula quinto = matricula(1, 1, "ZAPATA", "2027", "5to", 5);
        Matricula primero = matricula(2, 2, "ALVA", "2027", "1ro", 1);
        when(accesoContextoService.alcance("MATRICULAS", 7, roles)).thenReturn(Alcance.TODO);
        when(matriculaRepository.findFiltradasList(AccesoConstants.ELIMINADO, null, null, null, null, null, 2, null))
                .thenReturn(List.of(quinto, primero));
        MetodoPago yape = new MetodoPago();
        yape.setNombre("Yape");
        PagoMatricula pago = new PagoMatricula();
        pago.setTipo(TipoPago.values()[0]);
        pago.setMonto(new BigDecimal("100.00"));
        pago.setMetodoPago(yape);
        pago.setFecha(LocalDate.of(2026, 11, 1).atStartOfDay());
        when(pagoMatriculaRepository.findByMatriculaIdMatriculaAndAccesoNotOrderByFecha(any(), eq(eliminado)))
                .thenReturn(List.of(pago));

        List<MatriculaReporteResponse> filas = matriculas().reporte(null, null, 2, null, null, null, null, 7, roles);

        assertEquals(List.of("1ro A", "5to A"), filas.stream().map(MatriculaReporteResponse::getGradoSeccion).toList());
        assertEquals(0, new BigDecimal("200.00").compareTo(filas.get(0).getSaldo()));
        assertEquals("Yape", filas.get(0).getMetodoPago());
    }

    @Test
    void matriculaRespetaElRangoYElAlcance() {
        Matricula dentro = matricula(1, 1, "PEREZ", "2027", "2do", 2);
        Matricula fuera = matricula(2, 1, "PEREZ", "2027", "2do", 2);
        fuera.setFechaRegistro(LocalDate.of(2026, 1, 1));
        Matricula ajena = matricula(3, 9, "AJENO", "2027", "2do", 2);
        Apoderado apoderado = new Apoderado();
        when(accesoContextoService.alcance("MATRICULAS", 7, roles)).thenReturn(Alcance.APODERADO);
        when(accesoContextoService.apoderadoDeUsuario(7)).thenReturn(Optional.of(apoderado));
        when(alumnoApoderadoRepository.findByApoderado(apoderado)).thenReturn(List.of(dentro.getAlumnoApoderado()));
        when(matriculaRepository.findByAlumnoApoderadoAlumnoIdAlumnoAndAccesoNot(1, eliminado))
                .thenReturn(List.of(dentro, fuera));
        when(matriculaRepository.findFiltradasList(AccesoConstants.ELIMINADO, null, null, null, null, null, 2, null))
                .thenReturn(List.of(dentro, fuera, ajena));
        when(pagoMatriculaRepository.findByMatriculaIdMatriculaAndAccesoNotOrderByFecha(any(), eq(eliminado)))
                .thenReturn(List.of());

        List<MatriculaReporteResponse> filas = matriculas().reporte(LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 11, 30), 2, null, null, null, null, 7, roles);

        assertEquals(List.of(1), filas.stream().map(MatriculaReporteResponse::getIdMatricula).toList());
    }

    @Test
    void rangoSinLimitesAceptaFechaVaciaYConLimitesLaExcluye() {
        assertTrue(RangoFechas.contiene(null, null, null));
        assertFalse(RangoFechas.contiene(null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1)));
        assertTrue(RangoFechas.contiene(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1)));
    }
}
