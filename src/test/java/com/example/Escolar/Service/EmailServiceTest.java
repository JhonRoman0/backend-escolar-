package com.example.Escolar.Service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

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
                "alguien@gmail.com", "JHONATAN GUEVARA", "A20260001", "Clave1");

        assertTrue(enviado);
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void enviarCredencialesDevuelveFalseAnteErrorDeMensajeria() {
        when(mailSender.createMimeMessage()).thenReturn(mensaje());
        doThrow(new RuntimeException("Fallo SMTP")).when(mailSender).send(any(MimeMessage.class));

        boolean enviado = emailService.enviarCredencialesAcceso(
                "alguien@gmail.com", "JHONATAN GUEVARA", "A20260001", "Clave1");

        assertFalse(enviado);
    }
}