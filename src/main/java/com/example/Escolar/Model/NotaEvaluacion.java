package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Nota de un alumno en una evaluación, en la escala de su nivel. Borrarla solo cambia su estado. */
@Entity
@Table(name = "nota_evaluacion",
        uniqueConstraints = @UniqueConstraint(name = "uk_nota_evaluacion_matricula",
                columnNames = {"id_evaluacion", "id_matricula"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotaEvaluacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nota_evaluacion")
    Integer idNotaEvaluacion;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evaluacion", nullable = false)
    Evaluacion evaluacion;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_matricula", nullable = false)
    Matricula matricula;
    @Column(name = "calificacion", length = 3, nullable = false)
    String calificacion;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario_registro", nullable = false)
    Usuario usuarioRegistro;
    @Column(name = "fecha_registro", nullable = false)
    LocalDateTime fechaRegistro;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}
