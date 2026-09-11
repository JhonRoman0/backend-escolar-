package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "Nota")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Nota {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idNota;
    @ManyToOne
    @JoinColumn(name = "idMatricula", nullable = false)
    Matricula matricula;
    @ManyToOne
    @JoinColumn(name = "idCompetencia", nullable = false)
    Competencia competencia;
    @Column(name = "bimestre", nullable = false)
    byte bimestre;
    @Column(name = "calificacion", length = 3, nullable = false)
    String calificacion;
    @Column(name = "conclusionDescriptiva", columnDefinition = "TEXT")
    String conclusionDescriptiva;
    @Column(name = "fechaRegistro", nullable = false)
    LocalDate fechaRegistro;
    @ManyToOne
    @JoinColumn(name = "idUsuarioRegistro", nullable = false)
    Usuario usuarioRegistro;
    @Column(name = "acceso", nullable = false)
    byte acceso = 1;
}
