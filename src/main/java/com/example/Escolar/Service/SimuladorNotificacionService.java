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
    public void notificar(NotificacionAsistenciaDto evento) {
        log.info("[SIMULADOR NOTIFICACION] {} → correos {} celulares {}", MensajeAsistencia.texto(evento),
                evento.correos(), evento.celulares());
    }
}
