package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "AlumnoApoderado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlumnoApoderado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idAlumnoApoderado;
    @ManyToOne
    @JoinColumn(name = "idAlumno", nullable = false)
    Alumno alumno;
    @ManyToOne
    @JoinColumn(name = "idApoderado", nullable = false)
    Apoderado apoderado;
    @Column(name = "apoPrincipal", nullable = false)
    byte apoPrincipal;
}