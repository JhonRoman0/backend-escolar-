package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Asignacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Asignacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idAsignacion;
    @ManyToOne
    @JoinColumn(name = "idCurso", nullable = false)
    Curso curso;
    @ManyToOne
    @JoinColumn(name = "idDocente", nullable = false)
    Docente docente;
    @ManyToOne
    @JoinColumn(name = "idGradoSeccion", nullable = false)
    GradoSeccion gradoSeccion;
    @ManyToOne
    @JoinColumn(name = "idAnio", nullable = false)
    AnioEscolar anioEscolar;
    @Column(name = "acceso", length = 10, nullable = false)
    byte acceso;
}
