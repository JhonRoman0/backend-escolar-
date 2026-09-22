package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "galeriaDetalle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaleriaDetalle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_galeria_detalle")
    Integer idDetalle;
    @ManyToOne
    @JoinColumn(name = "id_galeria", nullable = false)
    GaleriaImagen galeria;
    @Column(name = "imagen_url", length = 255, nullable = false)
    String imagenUrl;
    @Column(name = "pk_url_foto", length = 255)
    String pkUrlFoto;
    @Column(name = "orden")
    Integer orden;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}