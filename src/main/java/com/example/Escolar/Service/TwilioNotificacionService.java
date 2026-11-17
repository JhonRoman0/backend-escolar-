package com.example.Escolar.Service;

import com.example.Escolar.Config.TwilioConfig;
import com.example.Escolar.Dto.NotificacionAsistenciaDto;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notificacion.proveedor", havingValue = "twilio")
public class TwilioNotificacionService implements NotificacionService {

    private final TwilioConfig twilioConfig;

    @Async
    @Override
    public void notificar(NotificacionAsistenciaDto evento) {
        String texto = "Control Escolar: " + MensajeAsistencia.texto(evento);
        evento.celulares().forEach(celular -> {
            try {
                String destino = celular.startsWith("+51") ? celular : "+51" + celular.trim();
                Message.creator(new PhoneNumber(destino), new PhoneNumber(twilioConfig.getTwilioPhoneNumber()), texto).create();
                log.info("SMS enviado correctamente a: {}", destino);
            } catch (Exception e) {
                log.error("Error al enviar el SMS a través de Twilio: {}", e.getMessage());
            }
        });
    }
}
