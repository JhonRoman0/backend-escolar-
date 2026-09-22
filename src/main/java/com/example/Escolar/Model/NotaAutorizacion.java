package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notaAutorizacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotaAutorizacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nota_autorizacion")
    Integer idNotaAutorizacion;
    @Column(name = "codigo_hash", length = 64, nullable = false, unique = true)
    String codigoHash;
    @Column(name = "codigo", length = 8)
    String codigo;
    @ManyToOne
    @JoinColumn(name = "id_usuario_emisor", nullable = false)
    Usuario usuarioEmisor;
    @ManyToOne
    @JoinColumn(name = "id_usuario_destinatario")
    Usuario usuarioDestinatario;
    @Column(name = "fecha_asignacion")
    LocalDateTime fechaAsignacion;
    @Column(name = "fecha_generacion", nullable = false)
    LocalDateTime fechaGeneracion;
    @Column(name = "fecha_expiracion", nullable = false)
    LocalDateTime fechaExpiracion;
    @ManyToOne
    @JoinColumn(name = "id_usuario_consumidor")
    Usuario usuarioConsumidor;
    @Column(name = "fecha_uso")
    LocalDateTime fechaUso;
    @Column(name = "usado", nullable = false)
    byte usado;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}