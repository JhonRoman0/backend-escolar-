package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Curso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Curso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idCurso;
    @Column(name = "nombre", length = 50, nullable = false, unique = true)
    String nombre;
    @Column(name = "acceso", length = 10, nullable = false)
    byte acceso;
}
