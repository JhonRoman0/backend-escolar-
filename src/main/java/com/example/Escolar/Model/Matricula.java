package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Matricula")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Matricula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idMatricula;
    @ManyToOne
    @JoinColumn(name = "idAlumnoApoderado", nullable = false)
    AlumnoApoderado alumnoApoderado;
    @ManyToOne
    @JoinColumn(name = "idUsuario", nullable = false)
    Usuario usuario;
    @ManyToOne
    @JoinColumn(name = "idGradoSeccion", nullable = false)
    GradoSeccion gradoSeccion;
    @Column(name = "solicitudMatricula", nullable = false)
    byte solicitudMatricula;
    @Column(name = "fechaPago")
    LocalDate fechaPago;
    @Column(name = "montoPago")
    BigDecimal montoPago;
    @Column(name = "fechaRegistro", nullable = false)
    LocalDate fechaRegistro;
    @Column(name = "acceso", nullable = false)
    byte acceso = 1;
}