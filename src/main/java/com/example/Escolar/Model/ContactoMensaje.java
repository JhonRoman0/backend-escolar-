package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "Contacto_Mensaje")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContactoMensaje {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idMensaje;
    @Column(name = "nombreRemitente", length = 100, nullable = false)
    String nombreRemitente;
    @Column(name = "correo", length = 100, nullable = false)
    String correo;
    @Column(name = "celular", length = 15)
    String celular;
    @Column(name = "asunto", length = 150)
    String asunto;
    @Column(name = "mensaje", columnDefinition = "TEXT", nullable = false)
    String mensaje;
    @Column(name = "fechaEnvio", nullable = false)
    LocalDateTime fechaEnvio;
    @Column(name = "estado", nullable = false)
    byte estado;
    @ManyToOne
    @JoinColumn(name = "idUsuario")
    Usuario usuario;
    @Column(name = "acceso", nullable = false)
    byte acceso = 1;
}