package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "NotaAutorizacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotaAutorizacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idNotaAutorizacion;
    @Column(name = "codigoHash", length = 64, nullable = false, unique = true)
    String codigoHash;
    @Column(name = "codigo", length = 8)
    String codigo;
    @ManyToOne
    @JoinColumn(name = "idUsuarioEmisor", nullable = false)
    Usuario usuarioEmisor;
    @ManyToOne
    @JoinColumn(name = "idUsuarioDestinatario")
    Usuario usuarioDestinatario;
    @Column(name = "fechaAsignacion")
    LocalDateTime fechaAsignacion;
    @Column(name = "fechaGeneracion", nullable = false)
    LocalDateTime fechaGeneracion;
    @Column(name = "fechaExpiracion", nullable = false)
    LocalDateTime fechaExpiracion;
    @ManyToOne
    @JoinColumn(name = "idUsuarioConsumidor")
    Usuario usuarioConsumidor;
    @Column(name = "fechaUso")
    LocalDateTime fechaUso;
    @Column(name = "usado", nullable = false)
    byte usado;
    @Column(name = "acceso", nullable = false)
    byte acceso = 1;
}