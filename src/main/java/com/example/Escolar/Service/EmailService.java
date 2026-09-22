package com.example.Escolar.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@escolar.edu.pe}")
    private String fromAddress;

    @Async
    public void enviarCodigoRecuperacion(String destino, String codigo) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(destino);
            helper.setSubject("Recuperación de contraseña - Escolar");

            String html = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                  <div style="max-width: 500px; margin: auto; background: #ffffff; border-radius: 8px; padding: 30px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                    <h2 style="color: #333; text-align: center;">Recuperación de Contraseña</h2>
                    <p style="color: #555; font-size: 15px; text-align: center;">
                      Ha solicitado restablecer su contraseña. Use el siguiente código:
                    </p>
                    <div style="text-align: center; margin: 30px 0;">
                      <span style="display: inline-block; background: #2563eb; color: #ffffff; font-size: 32px; font-weight: bold; letter-spacing: 8px; padding: 15px 30px; border-radius: 8px;">
                        %s
                      </span>
                    </div>
                    <p style="color: #777; font-size: 13px; text-align: center;">
                      Este código expira en 15 minutos.<br>
                      Si no solicitó este cambio, ignore este mensaje.
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(codigo);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Correo de recuperación enviado a {}", destino);
        } catch (MessagingException e) {
            log.error("Error al enviar correo de recuperación a {}: {}", destino, e.getMessage());
            throw new RuntimeException("No se pudo enviar el correo de recuperación");
        }
    }
}
