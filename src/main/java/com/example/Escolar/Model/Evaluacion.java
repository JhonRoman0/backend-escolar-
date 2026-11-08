package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Examen, práctica o trabajo de un curso en una sección y periodo. Su nota sirve
 * de guía para la nota final de cada competencia que marca.
 */
@Entity
@Table(name = "evaluacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Evaluacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluacion")
    Integer idEvaluacion;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_asignacion", nullable = false)
    Asignacion asignacion;
    /** Número del bimestre o trimestre. */
    @Column(name = "periodo", nullable = false)
    byte periodo;
    @Column(name = "nombre", length = 100, nullable = false)
    String nombre;
    @Column(name = "fecha", nullable = false)
    LocalDate fecha;
    @ManyToMany
    @JoinTable(name = "evaluacion_competencia",
            joinColumns = @JoinColumn(name = "id_evaluacion"),
            inverseJoinColumns = @JoinColumn(name = "id_competencia"))
    Set<Competencia> competencias = new HashSet<>();
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario_registro", nullable = false)
    Usuario usuarioRegistro;
    @Column(name = "fecha_registro", nullable = false)
    LocalDateTime fechaRegistro;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}
