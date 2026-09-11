package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Galeria_Detalle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaleriaDetalle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idDetalle;
    @ManyToOne
    @JoinColumn(name = "idGaleria", nullable = false)
    GaleriaImagen galeria;
    @Column(name = "imagenUrl", length = 255, nullable = false)
    String imagenUrl;
    @Column(name = "pkUrlFoto", length = 255)
    String pkUrlFoto;
    @Column(name = "orden")
    Integer orden;
    @Column(name = "acceso", nullable = false)
    byte acceso = 1;
}