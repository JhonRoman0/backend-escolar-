package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "Galeria_Imagen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaleriaImagen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idGaleria;
    @Column(name = "titulo", length = 150, nullable = false)
    String titulo;
    @Column(name = "descripcion", length = 255)
    String descripcion;
    @Column(name = "fecha", nullable = false)
    LocalDate fecha;
    @Column(name = "acceso", nullable = false)
    byte acceso = 1;
}