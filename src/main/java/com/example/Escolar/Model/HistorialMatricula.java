package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "HistorialMatricula")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistorialMatricula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idHistorial;
    @ManyToOne
    @JoinColumn(name = "idMatricula", nullable = false)
    Matricula matricula;
    @ManyToOne
    @JoinColumn(name = "idGradoSeccion", nullable = false)
    GradoSeccion gradoSeccion;
    @Column(name = "fechaInicio", nullable = false)
    LocalDate fechaInicio;
    @Column(name = "fechaFinal")
    LocalDate fechaFinal;
    @Column(name = "motivo", length = 150)
    String motivo;
    @Column(name = "acceso", nullable = false)
    byte acceso = 1;
}