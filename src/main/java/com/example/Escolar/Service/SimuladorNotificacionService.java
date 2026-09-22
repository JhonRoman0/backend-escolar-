package com.example.Escolar.Service;

import com.example.Escolar.Dto.NotificacionAsistenciaDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.notificacion.proveedor", havingValue = "simulador", matchIfMissing = true)
public class SimuladorNotificacionService implements NotificacionService {

    @Async
    @Override
    public void notificarMarcado(NotificacionAsistenciaDto evento) {
        evento.celulares().forEach(celular ->
                log.info("[SIMULADOR NOTIFICACION] Alumno {} marcó {} el {} a las {}. Mensaje simulado para el celular {}.",
                        evento.alumnoNombre(),
                        evento.estado(),
                        evento.fecha(),
                        evento.horaEntrada(),
                        celular));
    }
}