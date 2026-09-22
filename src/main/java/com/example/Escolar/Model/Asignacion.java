package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "asignacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Asignacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asignacion")
    Integer idAsignacion;
    @ManyToOne
    @JoinColumn(name = "id_curso", nullable = false)
    Curso curso;
    @ManyToOne
    @JoinColumn(name = "id_docente", nullable = false)
    Docente docente;
    @ManyToOne
    @JoinColumn(name = "id_grado_seccion", nullable = false)
    GradoSeccion gradoSeccion;
    @ManyToOne
    @JoinColumn(name = "id_anio", nullable = false)
    AnioEscolar anioEscolar;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}
