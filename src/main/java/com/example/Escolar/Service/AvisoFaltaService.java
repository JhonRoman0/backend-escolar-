package com.example.Escolar.Service;

import com.example.Escolar.Dto.NotificacionAsistenciaDto;
import com.example.Escolar.Model.*;
import com.example.Escolar.Repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cada pocos minutos avisa a los apoderados de los alumnos que no registraron
 * ingreso, una vez pasado el límite de tardanza de su turno. Solo en días de
 * clase del año vigente; un aviso por alumno y día (tabla aviso_falta).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvisoFaltaService {

    private final AnioEscolarRepository anioEscolarRepository;
    private final MatriculaRepository matriculaRepository;
    private final AsistenciaAlumnoRepository asistenciaRepository;
    private final AvisoFaltaRepository avisoRepository;
    private final AlumnoApoderadoRepository alumnoApoderadoRepository;
    private final AccesoRepository accesoRepository;
    private final CalendarioEscolar calendario;
    private final NotificacionService notificacionService;

    @Scheduled(fixedDelayString = "${app.asistencia.aviso-falta.cada-ms:300000}", initialDelay = 60000)
    @Transactional
    public void avisarFaltasDeHoy() {
        avisar(LocalDate.now(), LocalTime.now());
    }

    void avisar(LocalDate hoy, LocalTime ahora) {
        Acceso eliminado = accesoRepository.findById(AccesoConstants.ELIMINADO).orElseThrow();
        AnioEscolar anio = anioEscolarRepository.findByEstadoAndAccesoNot(AnioEscolarService.ESTADO_VIGENTE, eliminado).orElse(null);
        if (anio == null) {
            return;
        }
        // Solo alumnos cuyo turno ya pasó el límite de tardanza.
        List<Matricula> vencidas = matriculaRepository.findByGradoSeccionAnioEscolarIdAnioAndAccesoNot(anio.getIdAnio(), eliminado).stream()
                .filter(m -> AccesoConstants.ACTIVO.equals(m.getAcceso().getIdAcceso()))
                .filter(m -> ahora.isAfter(m.getGradoSeccion().getTurno().getHoraFaltaLimite()))
                .toList();
        if (vencidas.isEmpty()) {
            return;
        }
        Set<Integer> conMarca = asistenciaRepository.findByMatriculaIdMatriculaInAndFechaBetweenAndAccesoNot(
                        vencidas.stream().map(Matricula::getIdMatricula).toList(), hoy, hoy, eliminado).stream()
                .map(a -> a.getMatricula().getIdMatricula()).collect(Collectors.toSet());
        Set<Integer> avisadas = avisoRepository.findByFecha(hoy).stream()
                .map(a -> a.getMatricula().getIdMatricula()).collect(Collectors.toSet());
        Map<Integer, Boolean> claseHoy = new HashMap<>();
        int enviados = 0;
        for (Matricula m : vencidas) {
            if (conMarca.contains(m.getIdMatricula()) || avisadas.contains(m.getIdMatricula())) {
                continue;
            }
            Nivel nivel = m.getGradoSeccion().getGrado().getNivel();
            if (!claseHoy.computeIfAbsent(nivel.getIdNivel(), id -> calendario.esDiaDeClase(anio, nivel, hoy))) {
                continue;
            }
            AvisoFalta aviso = new AvisoFalta();
            aviso.setMatricula(m);
            aviso.setFecha(hoy);
            aviso.setFechaEnvio(LocalDateTime.now());
            avisoRepository.save(aviso);
            Alumno alumno = m.getAlumnoApoderado().getAlumno();
            List<AlumnoApoderado> vinculos = alumnoApoderadoRepository.findByAlumno(alumno);
            notificacionService.notificar(new NotificacionAsistenciaDto(NotificacionAsistenciaDto.Tipo.FALTA,
                    alumno.getNombre() + " " + alumno.getApellidoPat() + " " + alumno.getApellidoMat(), hoy, null,
                    AsistenciaService.ESTADO_INASISTENCIA, null, null,
                    vinculos.stream().map(v -> v.getApoderado().getCelular()).filter(c -> c != null && !c.isBlank()).toList(),
                    AsistenciaService.correosDe(vinculos)));
            enviados++;
        }
        if (enviados > 0) {
            log.info("Avisos de falta del {}: {}", hoy, enviados);
        }
    }
}
