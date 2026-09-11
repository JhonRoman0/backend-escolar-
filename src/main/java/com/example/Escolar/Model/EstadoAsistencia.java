package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "EstadoAsistencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstadoAsistencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idEstado;
    @Column(name = "nombre", length = 30, nullable = false, unique = true)
    String nombre;
    @Column(name = "acceso", nullable = false)
    byte acceso = 1;
}