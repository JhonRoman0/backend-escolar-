package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "galeriaImagen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaleriaImagen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_galeria_imagenen")
    Integer idGaleria;
    @Column(name = "titulo", length = 150, nullable = false)
    String titulo;
    @Column(name = "descripcion", length = 255)
    String descripcion;
    @Column(name = "fecha", nullable = false)
    LocalDate fecha;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}