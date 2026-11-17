package com.example.Escolar.Service;

import com.example.Escolar.Dto.AsistenciaRequest;
import com.example.Escolar.Dto.UploadResponse;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.*;
import com.example.Escolar.Service.AccesoContextoService.Alcance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AsistenciaRegistroTest {
    @Mock AsistenciaAlumnoRepository asistenciaAlumnoRepository;
    @Mock EstadoAsistenciaRepository estadoAsistenciaRepository;
    @Mock JustificacionRepository justificacionRepository;
    @Mock HistorialAsistenciaRepository historialAsistenciaRepository;
    @Mock AlumnoRepository alumnoRepository;
    @Mock AlumnoApoderadoRepository alumnoApoderadoRepository;
    @Mock MatriculaRepository matriculaRepository;
    @Mock AnioEscolarRepository anioEscolarRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock NotificacionService notificacionService;
    @Mock AccesoRepository accesoRepository;
    @Mock AccesoContextoService accesoContextoService;
    @Mock CalendarioEscolar calendario;
    @Mock CloudinaryService cloudinaryService;
    @Mock AvisoFaltaRepository avisoFaltaRepository;
    @InjectMocks AsistenciaService service;

    private final Acceso activo = new Acceso(AccesoConstants.ACTIVO, "Activo");
    private final Acceso eliminado = new Acceso(AccesoConstants.ELIMINADO, "Eliminado");
    private final List<String> roles = List.of("Auxiliar");
    private final LocalDate hoy = LocalDate.now();
    private Turno turno;
    private Alumno alumno;
    private Matricula matricula;
    private AnioEscolar anio;

    @BeforeEach
    void preparar() {
        when(accesoRepository.findById(AccesoConstants.ACTIVO)).thenReturn(Optional.of(activo));
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        anio = new AnioEscolar();
        anio.setIdAnio(1);
        anio.setAnio("2026");
        anio.setEstado(AnioEscolarService.ESTADO_VIGENTE);
        Nivel nivel = new Nivel();
        nivel.setIdNivel(2);
        nivel.setNombre("Primaria");
        Grado grado = new Grado();
        grado.setIdGrado(3);
        grado.setNombre("3ro");
        grado.setNivel(nivel);
        turno = new Turno();
        turno.setIdTurno(1);
        turno.setNombre("Mañana");
        GradoSeccion gs = new GradoSeccion();
        gs.setIdGradoSeccion(30);
        gs.setGrado(grado);
        gs.setTurno(turno);
        gs.setAnioEscolar(anio);
        alumno = new Alumno();
        alumno.setIdAlumno(9);
        alumno.setCodigo("A2026001");
        alumno.setNombre("ANA");
        alumno.setApellidoPat("PEREZ");
        alumno.setApellidoMat("RUIZ");
        alumno.setDocumentoIdentidad("71234567");
        alumno.setAcceso(activo);
        AlumnoApoderado vinculo = new AlumnoApoderado();
        vinculo.setAlumno(alumno);
        matricula = new Matricula();
        matricula.setIdMatricula(5);
        matricula.setGradoSeccion(gs);
        matricula.setAlumnoApoderado(vinculo);
        matricula.setAcceso(activo);

        when(alumnoRepository.findByCodigoAndAccesoNot("A2026001", eliminado)).thenReturn(Optional.of(alumno));
        when(accesoContextoService.matriculaActual(9)).thenReturn(Optional.of(matricula));
        when(accesoContextoService.alcance("ASISTENCIAS", 7, roles)).thenReturn(Alcance.TODO);
        when(accesoContextoService.docenteDeUsuario(7)).thenReturn(Optional.empty());
        when(calendario.esDiaDeClase(eq(anio), any(), any())).thenReturn(true);
        when(calendario.diasDeClase(eq(anio), any(), any(), any())).thenReturn(Set.of(hoy));
        when(usuarioRepository.findByIdUsuarioAndAccesoNot(7, eliminado)).thenReturn(Optional.of(new Usuario()));
        when(estadoAsistenciaRepository.findByNombreIgnoreCaseAndAccesoNot(anyString(), eq(eliminado)))
                .thenAnswer(i -> Optional.of(estado(i.getArgument(0))));
        Justificacion cita = new Justificacion();
        cita.setIdJustificacion(4);
        cita.setMotivo("Cita médica");
        when(justificacionRepository.findByIdJustificacionAndAccesoNot(4, eliminado)).thenReturn(Optional.of(cita));
        when(asistenciaAlumnoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(anioEscolarRepository.findByEstadoAndAccesoNot(AnioEscolarService.ESTADO_VIGENTE, eliminado)).thenReturn(Optional.of(anio));
        when(matriculaRepository.findByGradoSeccionAnioEscolarIdAnioAndAccesoNot(1, eliminado)).thenReturn(List.of(matricula));
    }

    private static EstadoAsistencia estado(String nombre) {
        EstadoAsistencia e = new EstadoAsistencia();
        e.setNombre(nombre);
        return e;
    }

    /** Turno ya pasado el límite de tardanza: solo entra justificado (salvo justo a medianoche). */
    private void pasadoElLimiteDeTardanza() {
        turno.setHoraEntrada(LocalTime.MIN);
        turno.setHoraEntradaLimite(LocalTime.MIN);
        turno.setHoraFaltaLimite(LocalTime.MIN);
        turno.setHoraSalida(LocalTime.MAX);
    }

    private AsistenciaRequest pedido(String acompanante) {
        AsistenciaRequest r = new AsistenciaRequest();
        r.setCodigo("A2026001");
        r.setIdJustificacion(4);
        r.setAcompanante(acompanante);
        return r;
    }

    private static MockMultipartFile foto() {
        return new MockMultipartFile("foto", "evidencia.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    @Test
    void ingresoJustificadoExigeQuienLoTrajo() {
        pasadoElLimiteDeTardanza();

        var ex = assertThrows(IllegalArgumentException.class, () -> service.confirmar(pedido(" "), foto(), 7, roles));

        assertTrue(ex.getMessage().contains("persona"), ex.getMessage());
        verify(asistenciaAlumnoRepository, never()).save(any());
    }

    @Test
    void ingresoJustificadoExigeLaFoto() {
        pasadoElLimiteDeTardanza();

        var ex = assertThrows(IllegalArgumentException.class, () -> service.confirmar(pedido("María López"), null, 7, roles));

        assertTrue(ex.getMessage().contains("foto"), ex.getMessage());
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void ingresoJustificadoGuardaAcompananteYEvidencia() {
        pasadoElLimiteDeTardanza();
        UploadResponse subida = new UploadResponse();
        subida.setUrl("https://img/evidencia.jpg");
        subida.setPublicId("asistencias/evidencias/abc");
        when(cloudinaryService.upload(any(), eq(AsistenciaService.CARPETA_EVIDENCIAS))).thenReturn(subida);

        var respuesta = service.confirmar(pedido("María López"), foto(), 7, roles);

        ArgumentCaptor<AsistenciaAlumno> guardada = ArgumentCaptor.forClass(AsistenciaAlumno.class);
        verify(asistenciaAlumnoRepository).save(guardada.capture());
        assertEquals(AsistenciaService.ESTADO_JUSTIFICADA, guardada.getValue().getEstado().getNombre());
        assertEquals("María López", guardada.getValue().getAcompanante());
        assertEquals("https://img/evidencia.jpg", guardada.getValue().getUrlEvidencia());
        assertEquals("María López", respuesta.getAcompanante());
    }

    @Test
    void elDocenteConPermisoRegistraSoloEnSusSecciones() {
        pasadoElLimiteDeTardanza();
        when(accesoContextoService.docenteDeUsuario(7)).thenReturn(Optional.of(new Docente()));
        when(accesoContextoService.gradoSeccionIdsDeDocente(7)).thenReturn(Set.of(99));
        UploadResponse subida = new UploadResponse();
        subida.setUrl("https://img/e.jpg");
        when(cloudinaryService.upload(any(), anyString())).thenReturn(subida);

        var ex = assertThrows(IllegalArgumentException.class, () -> service.confirmar(pedido("María López"), foto(), 7, roles));
        assertTrue(ex.getMessage().contains("secciones"), ex.getMessage());

        when(accesoContextoService.gradoSeccionIdsDeDocente(7)).thenReturn(Set.of(30));
        service.confirmar(pedido("María López"), foto(), 7, roles);
        verify(asistenciaAlumnoRepository).save(any());
    }

    @Test
    void anularPideMotivoYEsDelMismoDia() {
        AsistenciaAlumno marca = new AsistenciaAlumno();
        marca.setIdAsistencia(11);
        marca.setMatricula(matricula);
        marca.setFecha(hoy.minusDays(1));
        marca.setEstado(estado(AsistenciaService.ESTADO_PUNTUAL));
        marca.setAcceso(activo);
        when(asistenciaAlumnoRepository.findByIdAsistenciaAndAccesoNot(11, eliminado)).thenReturn(Optional.of(marca));

        assertThrows(IllegalArgumentException.class, () -> service.anular(11, "Se escaneó otro QR", 7, roles));

        marca.setFecha(hoy);
        assertThrows(IllegalArgumentException.class, () -> service.anular(11, "  ", 7, roles));

        service.anular(11, "Se escaneó otro QR", 7, roles);
        assertEquals(AccesoConstants.ELIMINADO, marca.getAcceso().getIdAcceso());
        // Queda como avisado: la tarea de faltas no le escribe al apoderado.
        verify(avisoFaltaRepository).save(any(AvisoFalta.class));
    }

    @Test
    void enUnDiaSinClasesNoSeRegistra() {
        when(calendario.esDiaDeClase(eq(anio), any(), any())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.confirmar(pedido(null), null, 7, roles));
        verify(asistenciaAlumnoRepository, never()).save(any());
    }

    @Test
    void soloConMatriculaEnElAnioVigente() {
        anio.setEstado(AnioEscolarService.ESTADO_CERRADO);

        var ex = assertThrows(IllegalArgumentException.class, () -> service.confirmar(pedido(null), null, 7, roles));

        assertTrue(ex.getMessage().contains("año escolar vigente"), ex.getMessage());
    }

    @Test
    void sinMarcaEstaPendienteHastaElLimiteYLuegoEsFalta() {
        Set<LocalDate> dias = Set.of(hoy, hoy.minusDays(1));
        turno.setHoraFaltaLimite(LocalTime.MAX);
        assertEquals(AsistenciaService.ESTADO_PENDIENTE, service.estadoSinMarca(matricula, hoy, dias));

        turno.setHoraFaltaLimite(LocalTime.MIN);
        assertEquals(AsistenciaService.ESTADO_INASISTENCIA, service.estadoSinMarca(matricula, hoy.minusDays(1), dias));
        assertEquals(AsistenciaService.ESTADO_SIN_CLASES, service.estadoSinMarca(matricula, hoy.minusDays(2), dias));
        assertNull(service.estadoSinMarca(matricula, hoy.plusDays(1), Set.of(hoy.plusDays(1))));
    }

    @Test
    void laBusquedaEncuentraPorDniYPorNombre() {
        turno.setHoraFaltaLimite(LocalTime.MAX);
        var porDni = service.asistenciasHoy(7, roles,
                new AsistenciaService.Filtro(null, null, null, null, null, null, "71234567", null), Pageable.unpaged());
        var porNombre = service.asistenciasHoy(7, roles,
                new AsistenciaService.Filtro(null, null, null, null, null, null, "ana perez", null), Pageable.unpaged());
        var otro = service.asistenciasHoy(7, roles,
                new AsistenciaService.Filtro(null, null, null, null, null, null, "luis", null), Pageable.unpaged());

        assertEquals(1, porDni.getContent().size());
        assertEquals(1, porNombre.getContent().size());
        assertTrue(otro.getContent().isEmpty());
        assertEquals(AsistenciaService.ESTADO_PENDIENTE, porDni.getContent().get(0).getEstado());
    }

    @Test
    void elFiltroDeEstadoFiltraDeVerdad() {
        turno.setHoraFaltaLimite(LocalTime.MAX);

        assertEquals(1, service.asistenciasHoy(7, roles, new AsistenciaService.Filtro(null, null, null, null, null, null, null,
                AsistenciaService.ESTADO_PENDIENTE), Pageable.unpaged()).getContent().size());
        assertTrue(service.asistenciasHoy(7, roles, new AsistenciaService.Filtro(null, null, null, null, null, null, null,
                AsistenciaService.ESTADO_TARDANZA), Pageable.unpaged()).getContent().isEmpty());
    }
}
