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
import org.springframework.web.util.HtmlUtils;

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

    /** Motivo del envío de credenciales: define el asunto y el mensaje del correo. */
    public enum TipoEnvioCredenciales { CREACION, RESTABLECIMIENTO, REACTIVACION }

    /**
     * Envía las credenciales de acceso (código y contraseña temporal) al crear,
     * reactivar o cambiar la contraseña de un usuario. Es síncrono a diferencia de
     * {@link #enviarCodigoRecuperacion} para que el flujo pueda reflejar el resultado
     * sin fallar la transacción: ante cualquier error se registra y se devuelve false.
     */
    public boolean enviarCredencialesAcceso(String destino, String nombre, String codigo, String contraseñaInicial,
                                            TipoEnvioCredenciales tipo) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(destino);
            helper.setSubject(tipo == TipoEnvioCredenciales.RESTABLECIMIENTO
                    ? "Nueva contraseña de acceso - " + NOMBRE_SISTEMA
                    : "Acceso al " + NOMBRE_SISTEMA);

            helper.setText(plantillaCredenciales(nombre, codigo, contraseñaInicial, tipo), true);
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
        return plantillaCodigo("Recuperación de Contraseña",
                "Ha solicitado restablecer su contraseña. Use el siguiente código:",
                codigo,
                "Este código expira en 15 minutos.<br>Si no solicitó este cambio, ignore este mensaje.");
    }

    /** Código grande centrado: lo usan la recuperación y el doble factor. */
    private String plantillaCodigo(String titulo, String introduccion, String codigo, String pie) {
        String contenido = """
                <h2 style="color: #333; text-align: center;">%s</h2>
                <p style="color: #555; font-size: 15px; text-align: center;">
                  %s
                </p>
                <div style="text-align: center; margin: 30px 0;">
                  <span style="display: inline-block; background: #2563eb; color: #ffffff; font-size: 32px; font-weight: bold; letter-spacing: 8px; padding: 15px 30px; border-radius: 8px;">
                    %s
                  </span>
                </div>
                <p style="color: #777; font-size: 13px; text-align: center;">
                  %s
                </p>
                """.formatted(titulo, introduccion, codigo, pie);
        return envolverHtml(contenido);
    }

    /**
     * Código para completar el inicio de sesión. Es síncrono: si no sale, el
     * usuario debe saberlo en el momento en vez de esperar un correo que no llega.
     */
    public boolean enviarCodigoDosFactor(String destino, String nombre, String codigo, int minutos,
                                         String navegador, String cuando) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(destino);
            helper.setSubject("Código de verificación - " + NOMBRE_SISTEMA);
            helper.setText(plantillaCodigo("Código de verificación",
                    "Hola <strong>" + esc(nombre) + "</strong>, use este código para terminar de iniciar sesión:",
                    esc(codigo),
                    "Este código expira en " + minutos + " minutos.<br>"
                            + "Solicitado desde " + esc(navegador) + " el " + esc(cuando) + ".<br>"
                            + "Si no fue usted, cambie su contraseña y comuníquese con el administrador."), true);
            mailSender.send(message);
            log.info("Código de verificación enviado a {}", destino);
            return true;
        } catch (Exception e) {
            log.error("Error al enviar el código de verificación a {}: {}", destino, e.getMessage());
            return false;
        }
    }

    /** Aviso de inicio de sesión desde un navegador nuevo. No es crítico: va aparte. */
    @Async
    public void enviarAvisoNuevoDispositivo(String destino, String nombre, String navegador, String ip, String cuando) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(destino);
            helper.setSubject("Nuevo inicio de sesión - " + NOMBRE_SISTEMA);
            String contenido = """
                    <h2 style="color: #333; text-align: center;">Nuevo inicio de sesión</h2>
                    <p style="color: #555; font-size: 15px; text-align: center;">
                      Hola <strong>%s</strong>, se inició sesión en su cuenta desde un dispositivo nuevo:
                    </p>
                    <table style="margin: 24px auto; font-size: 14px; color: #0f172a; border-collapse: collapse;">
                      <tr><td style="color: #64748b; padding: 4px 12px;">Fecha</td><td style="padding: 4px 12px;"><strong>%s</strong></td></tr>
                      <tr><td style="color: #64748b; padding: 4px 12px;">Dispositivo</td><td style="padding: 4px 12px;"><strong>%s</strong></td></tr>
                      <tr><td style="color: #64748b; padding: 4px 12px;">IP aproximada</td><td style="padding: 4px 12px;"><strong>%s</strong></td></tr>
                    </table>
                    <p style="color: #777; font-size: 13px; text-align: center;">
                      Si fue usted, no necesita hacer nada.<br>
                      Si no fue usted, cambie su contraseña y comuníquese con el administrador.
                    </p>
                    """.formatted(esc(nombre), esc(cuando), esc(navegador), esc(ip == null ? "desconocida" : ip));
            helper.setText(envolverHtml(contenido), true);
            mailSender.send(message);
            log.info("Aviso de nuevo inicio de sesión enviado a {}", destino);
        } catch (Exception e) {
            log.error("Error al enviar el aviso de nuevo inicio de sesión a {}: {}", destino, e.getMessage());
        }
    }

    private static String esc(String valor) {
        return HtmlUtils.htmlEscape(valor == null ? "" : valor, "UTF-8");
    }

    private String plantillaCredenciales(String nombre, String codigo, String contraseñaInicial,
                                         TipoEnvioCredenciales tipo) {
        String titulo = switch (tipo) {
            case CREACION -> "Bienvenido al " + NOMBRE_SISTEMA;
            case RESTABLECIMIENTO -> "Nueva contraseña de acceso";
            case REACTIVACION -> "Cuenta reactivada";
        };
        String saludo = "Hola <strong>" + HtmlUtils.htmlEscape(nombre, "UTF-8") + "</strong>, " + switch (tipo) {
            case CREACION -> "su cuenta de acceso fue creada con las siguientes credenciales:";
            case RESTABLECIMIENTO -> "el administrador asignó una nueva contraseña temporal a su cuenta:";
            case REACTIVACION -> "su cuenta fue reactivada con las siguientes credenciales:";
        };
        String contenido = """
                <h2 style="color: #333; text-align: center;">%s</h2>
                <p style="color: #555; font-size: 15px; text-align: center;">
                  %s
                </p>
                <div style="text-align: center; margin: 30px 0 0;">
                  <p style="color: #64748b; font-size: 13px; margin: 0 0 8px;">Código institucional</p>
                  <span style="display: inline-block; background: #2563eb; color: #ffffff; font-size: 26px; font-weight: bold; letter-spacing: 6px; padding: 14px 28px; border-radius: 8px;">
                    %s
                  </span>
                </div>
                <div style="text-align: center; margin: 24px 0 30px;">
                  <p style="color: #64748b; font-size: 13px; margin: 0 0 8px;">Contraseña temporal</p>
                  <span style="display: inline-block; background: #f1f5f9; color: #0f172a; font-size: 18px; font-weight: 600; padding: 10px 20px; border-radius: 8px;">
                    %s
                  </span>
                </div>
                <p style="color: #777; font-size: 13px; text-align: center;">
                  Por seguridad, deberá cambiar esta contraseña temporal al iniciar sesión por primera vez.<br>
                  Si usted no solicitó esta cuenta, ignore este mensaje.
                </p>
                """.formatted(titulo, saludo, HtmlUtils.htmlEscape(codigo, "UTF-8"),
                HtmlUtils.htmlEscape(contraseñaInicial, "UTF-8"));
        return envolverHtml(contenido);
    }
}
