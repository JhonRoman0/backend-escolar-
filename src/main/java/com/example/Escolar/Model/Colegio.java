package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "colegio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Colegio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_colegio")
    Integer idColegio;
    @Column(name = "nombre", length = 120, nullable = false)
    String nombre;
    @Column(name = "celular", length = 15)
    String celular;
    @Column(name = "telefono", length = 15)
    String telefono;
    @Column(name = "direccion", length = 150)
    String direccion;
    @Column(name = "codigo_colegio", length = 20)
    String codigoColegio;
    @Column(name = "url_foto", length = 255)
    String urlFoto;
    @Column(name = "pk_url_foto", length = 255)
    String pkUrlFoto;
    @Column(name = "url_portal", length = 255)
    String urlPortal;
    @Column(name = "pk_url_portada", length = 255)
    String pkUrlPortada;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}