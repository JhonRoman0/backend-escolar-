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

    private static final String NOMBRE_SISTEMA = "Sistema Escolar";

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
            helper.setSubject("Recuperación de contraseña - " + NOMBRE_SISTEMA);

            helper.setText(plantillaRecuperacion(codigo), true);
            mailSender.send(message);
            log.info("Correo de recuperación enviado a {}", destino);
        } catch (MessagingException e) {
            log.error("Error al enviar correo de recuperación a {}: {}", destino, e.getMessage());
            throw new RuntimeException("No se pudo enviar el correo de recuperación");
        }
    }

    /**
     * Envía las credenciales de acceso (código y contraseña inicial) al crear o
     * reactivar un usuario. Es síncrono a diferencia de {@link #enviarCodigoRecuperacion}
     * para que el flujo de creación pueda reflejar el resultado sin fallar la
     * transacción: ante cualquier error se registra y se devuelve false.
     */
    public boolean enviarCredencialesAcceso(String destino, String nombre, String codigo, String contraseñaInicial) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(destino);
            helper.setSubject("Acceso al " + NOMBRE_SISTEMA);

            helper.setText(plantillaCredenciales(nombre, codigo, contraseñaInicial), true);
            mailSender.send(message);
            log.info("Credenciales de acceso enviadas a {}", destino);
            return true;
        } catch (Exception e) {
            log.error("Error al enviar credenciales de acceso a {}: {}", destino, e.getMessage());
            return false;
        }
    }

    private String envolverHtml(String contenido) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
              <div style="max-width: 500px; margin: auto; background: #ffffff; border-radius: 8px; padding: 30px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
            %s
              </div>
            </body>
            </html>
            """.formatted(contenido);
    }

    private String plantillaRecuperacion(String codigo) {
        String contenido = """
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
                """.formatted(codigo);
        return envolverHtml(contenido);
    }

    private String plantillaCredenciales(String nombre, String codigo, String contraseñaInicial) {
        String contenido = """
                <h2 style="color: #333; text-align: center;">Bienvenido al %s</h2>
                <p style="color: #555; font-size: 15px; text-align: center;">
                  Hola <strong>%s</strong>, su cuenta de acceso fue creada con las siguientes credenciales:
                </p>
                <div style="text-align: center; margin: 30px 0;">
                  <span style="display: inline-block; background: #2563eb; color: #ffffff; font-size: 26px; font-weight: bold; letter-spacing: 6px; padding: 14px 28px; border-radius: 8px;">
                    %s
                  </span>
                </div>
                <div style="text-align: center; margin: 20px 0;">
                  <span style="display: inline-block; background: #f1f5f9; color: #0f172a; font-size: 18px; font-weight: 600; padding: 10px 20px; border-radius: 8px;">
                    %s
                  </span>
                </div>
                <p style="color: #777; font-size: 13px; text-align: center;">
                  Le recomendamos cambiar su contraseña en su primera sesión.<br>
                  Si usted no solicitó esta cuenta, ignore este mensaje.
                </p>
                """.formatted(NOMBRE_SISTEMA, nombre, codigo, contraseñaInicial);
        return envolverHtml(contenido);
    }
}
