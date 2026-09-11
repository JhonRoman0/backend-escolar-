package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "Publicacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Publicacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idPublicacion;
    @ManyToOne
    @JoinColumn(name = "idUsuario", nullable = false)
    Usuario usuario;
    @Column(name = "titulo", length = 150, nullable = false)
    String titulo;
    @Column(name = "slug", length = 200)
    String slug;
    @Column(name = "contenido", columnDefinition = "TEXT")
    String contenido;
    @Column(name = "imagenPortadaUrl", length = 255)
    String imagenPortadaUrl;
    @Column(name = "pkUrlFoto", length = 255)
    String pkUrlFoto;
    @Column(name = "categoria", length = 50)
    String categoria;
    @Column(name = "esDestacado", nullable = false)
    byte esDestacado;
    @Column(name = "estado", nullable = false)
    byte estado;
    @Column(name = "fechaPublicacion", nullable = false)
    LocalDateTime fechaPublicacion;
    @Column(name = "fechaActualizacion")
    LocalDateTime fechaActualizacion;
    @Column(name = "acceso", nullable = false)
    byte acceso = 1;
}