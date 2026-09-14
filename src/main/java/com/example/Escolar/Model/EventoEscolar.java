package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "Evento_Escolar")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventoEscolar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idEvento;
    @Column(name = "titulo", length = 150, nullable = false)
    String titulo;
    @Column(name = "descripcion", columnDefinition = "TEXT")
    String descripcion;
    @Column(name = "lugar", length = 120)
    String lugar;
    @Column(name = "fechaInicio", nullable = false)
    LocalDateTime fechaInicio;
    @Column(name = "fechaFin")
    LocalDateTime fechaFin;
    @Column(name = "esPublico", nullable = false)
    byte esPublico;
    @Column(name = "imagenUrl", length = 255)
    String imagenUrl;
    @Column(name = "pkUrlFoto", length = 255)
    String pkUrlFoto;
    @Column(name = "acceso", nullable = false)
    byte acceso = 1;
}