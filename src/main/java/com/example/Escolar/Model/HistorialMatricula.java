package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "historialMatricula")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistorialMatricula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial_matricula")
    Integer idHistorial;
    @ManyToOne
    @JoinColumn(name = "id_matricula", nullable = false)
    Matricula matricula;
    @ManyToOne
    @JoinColumn(name = "id_grado_seccion", nullable = false)
    GradoSeccion gradoSeccion;
    @Column(name = "fecha_inicio", nullable = false)
    LocalDate fechaInicio;
    @Column(name = "fecha_final")
    LocalDate fechaFinal;
    @Column(name = "motivo", length = 150)
    String motivo;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}