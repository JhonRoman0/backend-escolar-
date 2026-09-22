package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "publicacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Publicacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_publicacion")
    Integer idPublicacion;
    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    Usuario usuario;
    @Column(name = "titulo", length = 150, nullable = false)
    String titulo;
    @Column(name = "slug", length = 200)
    String slug;
    @Column(name = "contenido", columnDefinition = "TEXT")
    String contenido;
    @Column(name = "imagen_portada_url", length = 255)
    String imagenPortadaUrl;
    @Column(name = "pk_url_foto", length = 255)
    String pkUrlFoto;
    @Column(name = "categoria", length = 50)
    String categoria;
    @Column(name = "es_destacado", nullable = false)
    byte esDestacado;
    @Column(name = "estado", nullable = false)
    byte estado;
    @Column(name = "fecha_publicacion", nullable = false)
    LocalDateTime fechaPublicacion;
    @Column(name = "fecha_actualizacion")
    LocalDateTime fechaActualizacion;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}