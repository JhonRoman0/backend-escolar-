package com.example.Escolar.Service;

import com.example.Escolar.Dto.NotificacionAsistenciaDto;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Avisos de asistencia por correo, con la cuenta de las credenciales. Un fallo
 * solo se anota: la asistencia ya quedó guardada. Lleva la cuenta diaria de
 * correos para vigilar el límite de Gmail (unos 500 al día).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notificacion.proveedor", havingValue = "correo")
public class CorreoNotificacionService implements NotificacionService {

    private final JavaMailSender mailSender;
    private final AtomicInteger enviadosHoy = new AtomicInteger();
    private volatile LocalDate dia = LocalDate.now();

    @Value("${app.mail.from:noreply@escolar.edu.pe}")
    private String fromAddress;

    @Async
    @Override
    public void notificar(NotificacionAsistenciaDto evento) {
        for (String correo : evento.correos()) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
                helper.setFrom(fromAddress);
                helper.setTo(correo);
                helper.setSubject(MensajeAsistencia.asunto(evento) + " - Sistema Escolar");
                helper.setText(html(evento), true);
                mailSender.send(message);
                log.info("Aviso de asistencia enviado a {} ({} correos hoy)", correo, contar());
            } catch (Exception e) {
                log.error("No se pudo enviar el aviso de asistencia a {}: {}", correo, e.getMessage());
            }
        }
    }

    private synchronized int contar() {
        if (!LocalDate.now().equals(dia)) {
            dia = LocalDate.now();
            enviadosHoy.set(0);
        }
        return enviadosHoy.incrementAndGet();
    }

    private static String html(NotificacionAsistenciaDto evento) {
        return """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
              <div style="max-width: 500px; margin: auto; background: #ffffff; border-radius: 8px; padding: 24px;">
                <h3 style="color: #333; margin-top: 0;">Control de asistencia</h3>
                <p style="color: #444; font-size: 15px;">%s</p>
                <p style="color: #888; font-size: 12px;">Mensaje automático del Sistema Escolar.</p>
              </div>
            </body></html>
            """.formatted(HtmlUtils.htmlEscape(MensajeAsistencia.texto(evento), "UTF-8"));
    }
}
