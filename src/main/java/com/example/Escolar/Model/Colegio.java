package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Colegio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Colegio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idColegio;
    @Column(name = "nombre", length = 120, nullable = false)
    String nombre;
    @Column(name = "celular", length = 15)
    String celular;
    @Column(name = "telefono", length = 15)
    String telefono;
    @Column(name = "direccion", length = 150)
    String direccion;
    @Column(name = "codigoColegio", length = 20)
    String codigoColegio;
    @Column(name = "urlFoto", length = 255)
    String urlFoto;
    @Column(name = "pkUrlFoto", length = 255)
    String pkUrlFoto;
    @Column(name = "urlPortal", length = 255)
    String urlPortal;
    @Column(name = "pkUrlPortada", length = 255)
    String pkUrlPortada;
    @Column(name = "acceso", nullable = false)
    byte acceso = 1;
}