package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "nota")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Nota {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nota")
    Integer idNota;
    @ManyToOne
    @JoinColumn(name = "id_matricula", nullable = false)
    Matricula matricula;
    @ManyToOne
    @JoinColumn(name = "id_competencia", nullable = false)
    Competencia competencia;
    @Column(name = "bimestre", nullable = false)
    byte bimestre;
    @Column(name = "calificacion", length = 3, nullable = false)
    String calificacion;
    @Column(name = "conclusion_descriptiva", columnDefinition = "TEXT")
    String conclusionDescriptiva;
    @Column(name = "fecha_registro", nullable = false)
    LocalDate fechaRegistro;
    @ManyToOne
    @JoinColumn(name = "id_usuario_registro", nullable = false)
    Usuario usuarioRegistro;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}
