package com.example.Escolar.Service;

import com.example.Escolar.Dto.NotificacionAsistenciaDto;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AvisoFaltaServiceTest {
    @Mock AnioEscolarRepository anioEscolarRepository;
    @Mock MatriculaRepository matriculaRepository;
    @Mock AsistenciaAlumnoRepository asistenciaRepository;
    @Mock AvisoFaltaRepository avisoRepository;
    @Mock AlumnoApoderadoRepository alumnoApoderadoRepository;
    @Mock AccesoRepository accesoRepository;
    @Mock CalendarioEscolar calendario;
    @Mock NotificacionService notificacionService;
    @InjectMocks AvisoFaltaService service;

    private final Acceso activo = new Acceso(AccesoConstants.ACTIVO, "Activo");
    private final Acceso eliminado = new Acceso(AccesoConstants.ELIMINADO, "Eliminado");
    private final LocalDate hoy = LocalDate.of(2026, 3, 9);
    private final AnioEscolar anio = new AnioEscolar();
    private Matricula ana;

    @BeforeEach
    void preparar() {
        anio.setIdAnio(1);
        when(accesoRepository.findById(AccesoConstants.ELIMINADO)).thenReturn(Optional.of(eliminado));
        when(anioEscolarRepository.findByEstadoAndAccesoNot(AnioEscolarService.ESTADO_VIGENTE, eliminado)).thenReturn(Optional.of(anio));
        Turno manana = new Turno();
        manana.setHoraFaltaLimite(LocalTime.of(8, 30));
        Nivel nivel = new Nivel();
        nivel.setIdNivel(2);
        Grado grado = new Grado();
        grado.setNivel(nivel);
        GradoSeccion gs = new GradoSeccion();
        gs.setTurno(manana);
        gs.setGrado(grado);
        Alumno alumno = new Alumno();
        alumno.setNombre("ANA");
        alumno.setApellidoPat("PEREZ");
        alumno.setApellidoMat("RUIZ");
        AlumnoApoderado vinculo = new AlumnoApoderado();
        vinculo.setAlumno(alumno);
        Usuario usuarioApoderado = new Usuario();
        usuarioApoderado.setGmail("mama@correo.com");
        Apoderado apoderado = new Apoderado();
        apoderado.setUsuario(usuarioApoderado);
        vinculo.setApoderado(apoderado);
        ana = new Matricula();
        ana.setIdMatricula(5);
        ana.setGradoSeccion(gs);
        ana.setAlumnoApoderado(vinculo);
        ana.setAcceso(activo);
        when(matriculaRepository.findByGradoSeccionAnioEscolarIdAnioAndAccesoNot(1, eliminado)).thenReturn(List.of(ana));
        when(alumnoApoderadoRepository.findByAlumno(alumno)).thenReturn(List.of(vinculo));
        when(calendario.esDiaDeClase(eq(anio), any(), eq(hoy))).thenReturn(true);
    }

    @Test
    void pasadoElLimiteAvisaPorCorreoUnaVez() {
        service.avisar(hoy, LocalTime.of(8, 31));

        ArgumentCaptor<NotificacionAsistenciaDto> aviso = ArgumentCaptor.forClass(NotificacionAsistenciaDto.class);
        verify(notificacionService).notificar(aviso.capture());
        assertEquals(NotificacionAsistenciaDto.Tipo.FALTA, aviso.getValue().tipo());
        assertEquals(List.of("mama@correo.com"), aviso.getValue().correos());
        verify(avisoRepository).save(any());
    }

    @Test
    void antesDelLimiteNoAvisa() {
        service.avisar(hoy, LocalTime.of(8, 30));

        verifyNoInteractions(notificacionService);
    }

    @Test
    void siYaMarcoNoAvisa() {
        AsistenciaAlumno marca = new AsistenciaAlumno();
        marca.setMatricula(ana);
        when(asistenciaRepository.findByMatriculaIdMatriculaInAndFechaBetweenAndAccesoNot(any(), eq(hoy), eq(hoy), eq(eliminado)))
                .thenReturn(List.of(marca));

        service.avisar(hoy, LocalTime.of(9, 0));

        verifyNoInteractions(notificacionService);
    }

    @Test
    void noRepiteElAvisoDelDia() {
        AvisoFalta enviado = new AvisoFalta();
        enviado.setMatricula(ana);
        when(avisoRepository.findByFecha(hoy)).thenReturn(List.of(enviado));

        service.avisar(hoy, LocalTime.of(9, 0));

        verifyNoInteractions(notificacionService);
    }

    @Test
    void enUnDiaSinClasesNoAvisa() {
        when(calendario.esDiaDeClase(eq(anio), any(), eq(hoy))).thenReturn(false);

        service.avisar(hoy, LocalTime.of(9, 0));

        verifyNoInteractions(notificacionService);
        verify(avisoRepository, never()).save(any());
    }
}
