package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ContactoMensajeResponse {
    private Integer idMensaje;
    private String nombreRemitente;
    private String correo;
    private String celular;
    private String asunto;
    private String mensaje;
    private LocalDateTime fechaEnvio;
    private Byte estado;
    private Integer idUsuario;
    private String atendidoPor;
    private Byte acceso;
}