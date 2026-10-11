package com.example.Escolar.Service;

import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.example.Escolar.Service.EmailService.TipoEnvioCredenciales;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@escolar.edu.pe");
    }

    private MimeMessage mensaje() {
        return new MimeMessage(Session.getDefaultInstance(new Properties()));
    }

    @Test
    void enviarCredencialesDevuelveTrueYCreaElCorreo() {
        when(mailSender.createMimeMessage()).thenReturn(mensaje());

        boolean enviado = emailService.enviarCredencialesAcceso(
                "alguien@gmail.com", "JHONATAN GUEVARA", "A20260001", "Clave1", TipoEnvioCredenciales.CREACION);

        assertTrue(enviado);
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void correoDeCredencialesEtiquetaCodigoYContrasenaYEscapaCaracteres() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mensaje());

        emailService.enviarCredencialesAcceso(
                "alguien@gmail.com", "JHONATAN GUEVARA", "A20260001", "Ab1<x&y>", TipoEnvioCredenciales.CREACION);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        String html = extraerHtml(captor.getValue().getContent());

        assertTrue(html.contains("Código institucional"));
        assertTrue(html.contains("Contraseña temporal"));
        assertTrue(html.indexOf("Código institucional") < html.indexOf("A20260001"));
        assertTrue(html.indexOf("A20260001") < html.indexOf("Contraseña temporal"));
        assertTrue(html.contains("Ab1&lt;x&amp;y&gt;"));
        assertFalse(html.contains("Ab1<x&y>"));
        assertTrue(html.contains("deberá cambiar esta contraseña temporal"));
    }

    @Test
    void cadaTipoDeEnvioUsaSuAsuntoYMensaje() throws Exception {
        assertAsuntoYMensaje(TipoEnvioCredenciales.CREACION, "Acceso al Sistema Escolar",
                "su cuenta de acceso fue creada con las siguientes credenciales:");
        assertAsuntoYMensaje(TipoEnvioCredenciales.RESTABLECIMIENTO, "Nueva contraseña de acceso - Sistema Escolar",
                "el administrador asignó una nueva contraseña temporal a su cuenta:");
        assertAsuntoYMensaje(TipoEnvioCredenciales.REACTIVACION, "Acceso al Sistema Escolar",
                "su cuenta fue reactivada con las siguientes credenciales:");
    }

    private void assertAsuntoYMensaje(TipoEnvioCredenciales tipo, String asunto, String mensaje) throws Exception {
        MimeMessage correo = mensaje();
        when(mailSender.createMimeMessage()).thenReturn(correo);

        emailService.enviarCredencialesAcceso("alguien@gmail.com", "JHONATAN GUEVARA", "A20260001", "Clave1@x", tipo);

        assertEquals(asunto, correo.getSubject());
        String html = extraerHtml(correo.getContent());
        assertTrue(html.contains("Hola <strong>JHONATAN GUEVARA</strong>, " + mensaje), tipo.name());
        assertTrue(html.contains("Código institucional"), tipo.name());
        assertTrue(html.contains("Contraseña temporal"), tipo.name());
    }

    private String extraerHtml(Object contenido) throws Exception {
        if (contenido instanceof String texto) {
            return texto;
        }
        StringBuilder html = new StringBuilder();
        if (contenido instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                html.append(extraerHtml(multipart.getBodyPart(i).getContent()));
            }
        }
        return html.toString();
    }

    @Test
    void enviarCredencialesDevuelveFalseAnteErrorDeMensajeria() {
        when(mailSender.createMimeMessage()).thenReturn(mensaje());
        doThrow(new RuntimeException("Fallo SMTP")).when(mailSender).send(any(MimeMessage.class));

        boolean enviado = emailService.enviarCredencialesAcceso(
                "alguien@gmail.com", "JHONATAN GUEVARA", "A20260001", "Clave1", TipoEnvioCredenciales.CREACION);

        assertFalse(enviado);
    }
}