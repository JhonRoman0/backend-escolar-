package com.example.Escolar.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    Integer idUsuario;
    @Column(name = "nombre",length =30,nullable = false)
    String nombre;
    @Column(name = "apellido_pat",length = 30,nullable = false)
    String apellidoPat;
    @Column(name = "apellido_mat", length = 30,nullable = false)
    String apellidoMat;
    @Column(name = "codigo",length = 10,nullable = false)
    String codigo;
    @Column(name = "documento_identidad", length = 20, unique = true)
    String documentoIdentidad;
    @Column(name = "contraseña",nullable = false)
    String contraseña;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
    @Column(name = "gmail",length = 60,nullable = true)
    @Email
    String gmail;
    @Column(name = "fecha_naci",nullable = false)
    LocalDate fechaNaci;
    @Column(name = "url_foto",nullable = true)
    String urlFoto;
    @Column(name = "pk_url_foto",nullable = true)
    String pkUrlFoto;
    @Column(name = "fecha_creacion", nullable = false)
    LocalDate fechaCreacion;
    @Column(name = "intentos_fallidos", nullable = false)
    Integer intentosFallidos = 0;
    @Column(name = "fecha_bloqueo", nullable = true)
    LocalDateTime fechaBloqueo;
    @Column(name = "reset_token", length = 255, nullable = true)
    String resetToken;
    @Column(name = "reset_token_expiracion", nullable = true)
    LocalDateTime resetTokenExpiracion;

}
