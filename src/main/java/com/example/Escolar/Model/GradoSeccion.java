package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "GradoSeccion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GradoSeccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idGradoSeccion;
    @ManyToOne
    @JoinColumn(name = "idGrado", nullable = false)
    Grado grado;
    @ManyToOne
    @JoinColumn(name = "idSeccion", nullable = true)
    Seccion seccion;
    @ManyToOne
    @JoinColumn(name = "idTurno", nullable = false)
    Turno turno;
    @ManyToOne
    @JoinColumn(name = "idAnio", nullable = false)
    AnioEscolar anioEscolar;
    @Column(name = "acceso", length = 10, nullable = false)
    byte acceso;
}