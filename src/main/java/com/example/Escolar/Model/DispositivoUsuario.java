package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Navegador desde el que un usuario ya inició sesión. Sirve para avisar cuando
 * entra desde uno nuevo y, si marcó "recordar", para no pedirle el código
 * durante unos días.
 */
@Entity
@Table(name = "dispositivo_usuario",
        uniqueConstraints = @UniqueConstraint(name = "uk_dispositivo_usuario_token",
                columnNames = {"id_usuario", "token_hash"}))
@Getter
@Setter
@NoArgsConstructor
public class DispositivoUsuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dispositivo")
    Integer idDispositivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    Usuario usuario;

    /** Hash del valor de la cookie del navegador (nunca el valor). */
    @Column(name = "token_hash", length = 64, nullable = false)
    String tokenHash;

    /** Texto corto: "Chrome en Windows". */
    @Column(name = "navegador", length = 80, nullable = false)
    String navegador;

    @Column(name = "ip", length = 45, nullable = true)
    String ip;

    @Column(name = "primer_uso", nullable = false)
    LocalDateTime primerUso;

    @Column(name = "ultimo_uso", nullable = false)
    LocalDateTime ultimoUso;

    /** Hasta cuándo no se pide el código; vacío si no es de confianza. */
    @Column(name = "confiable_hasta", nullable = true)
    LocalDateTime confiableHasta;

    /**
     * Huella de la contraseña, el correo y la versión de sesión al marcarlo de
     * confianza: si cualquiera cambia, la confianza deja de valer sola.
     */
    @Column(name = "huella_confianza", length = 64, nullable = true)
    String huellaConfianza;
}
