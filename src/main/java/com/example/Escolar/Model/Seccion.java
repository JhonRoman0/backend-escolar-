package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Seccion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Seccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idSeccion;
    @Column(name = "nombre", length = 50, nullable = false)
    String nombre;
    @Column(name = "acceso", length = 10, nullable = false)
    byte acceso;
}
