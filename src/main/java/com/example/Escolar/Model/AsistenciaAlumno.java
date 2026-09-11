package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "AsistenciaAlumno",
        uniqueConstraints = @UniqueConstraint(name = "uk_matricula_fecha_acceso",
                columnNames = {"idMatricula", "fecha", "acceso"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AsistenciaAlumno {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idAsistencia;
    @ManyToOne
    @JoinColumn(name = "idMatricula", nullable = false)
    Matricula matricula;
    @ManyToOne
    @JoinColumn(name = "idEstado", nullable = false)
    EstadoAsistencia estado;
    @ManyToOne
    @JoinColumn(name = "idJustificacion")
    Justificacion justificacion;
    @ManyToOne
    @JoinColumn(name = "idUsuarioRegistro", nullable = false)
    Usuario usuarioRegistro;
    @Column(name = "fecha", nullable = false)
    LocalDate fecha;
    @Column(name = "horaEntrada", nullable = false)
    LocalTime horaEntrada;
    @Column(name = "horaSalida")
    LocalTime horaSalida;
    @Column(name = "acceso", nullable = false)
    byte acceso = 1;
}