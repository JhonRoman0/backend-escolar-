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
}
