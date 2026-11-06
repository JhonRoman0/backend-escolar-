package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "grado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Grado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_grado")
    Integer idGrado;
    @Column(name = "nombre", length = 50, nullable = false)
    String nombre;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
    @ManyToOne
    @JoinColumn(name = "id_nivel", nullable = false)
    Nivel nivel;
    // Posición dentro del nivel; la asigna el sistema. Grados con el mismo nombre y nivel comparten orden.
    @Column(name = "orden")
    Integer orden;
    // false: sus alumnos pasan al grado siguiente sin esperar el cierre del año.
    @Column(name = "repite")
    Boolean repite;
}
