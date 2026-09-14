package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Competencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Competencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idCompetencia;
    @ManyToOne
    @JoinColumn(name = "idCurso", nullable = false)
    Curso curso;
    @Column(name = "nombre", length = 150, nullable = false)
    String nombre;
    @Column(name = "orden", nullable = false)
    byte orden;
    @Column(name = "acceso", nullable = false)
    byte acceso = 1;
}
