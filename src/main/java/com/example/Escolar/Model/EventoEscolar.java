package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "eventoEscolar")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventoEscolar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento_escolar")
    Integer idEvento;
    @Column(name = "titulo", length = 150, nullable = false)
    String titulo;
    @Column(name = "descripcion", columnDefinition = "TEXT")
    String descripcion;
    @Column(name = "lugar", length = 120)
    String lugar;
    @Column(name = "fecha_inicio", nullable = false)
    LocalDateTime fechaInicio;
    @Column(name = "fecha_fin")
    LocalDateTime fechaFin;
    @Column(name = "es_publico", nullable = false)
    byte esPublico;
    @Column(name = "imagen_url", length = 255)
    String imagenUrl;
    @Column(name = "pk_url_foto", length = 255)
    String pkUrlFoto;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}