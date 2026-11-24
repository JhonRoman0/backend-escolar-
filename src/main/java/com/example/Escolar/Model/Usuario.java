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
    @Column(name = "celular", length = 15, nullable = true)
    String celular;
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
    @Column(name = "reset_token_enviado", nullable = true)
    LocalDateTime resetTokenEnviado;
    @Column(name = "intentos_codigo_reset", nullable = false, columnDefinition = "int default 0")
    Integer intentosCodigoReset = 0;
    // Contraseña asignada por el admin: se exige cambiarla al iniciar sesión.
    @Column(name = "debe_cambiar_contrasena", nullable = false, columnDefinition = "boolean default false")
    Boolean debeCambiarContrasena = false;
    @Column(name = "fecha_inicio_vigencia", nullable = false)
    LocalDate fechaInicioVigencia;
    @Column(name = "fecha_fin_vigencia", nullable = true)
    LocalDate fechaFinVigencia;
    @Column(name = "version_token", nullable = false, columnDefinition = "int default 0")
    Integer versionToken = 0;

    // Doble factor: el desafío identifica el inicio de sesión a medias; el código
    // va al correo. Ambos se guardan como hash.
    @Column(name = "dos_factor_desafio", length = 64, nullable = true)
    String dosFactorDesafio;
    @Column(name = "dos_factor_desafio_expiracion", nullable = true)
    LocalDateTime dosFactorDesafioExpiracion;
    @Column(name = "dos_factor_codigo", length = 64, nullable = true)
    String dosFactorCodigo;
    @Column(name = "dos_factor_codigo_expiracion", nullable = true)
    LocalDateTime dosFactorCodigoExpiracion;
    @Column(name = "dos_factor_enviado", nullable = true)
    LocalDateTime dosFactorEnviado;
    // Envíos en la hora en curso (tope para no agotar el cupo del correo).
    @Column(name = "dos_factor_envios", nullable = true)
    Integer dosFactorEnvios;
    @Column(name = "dos_factor_ventana", nullable = true)
    LocalDateTime dosFactorVentana;

}
