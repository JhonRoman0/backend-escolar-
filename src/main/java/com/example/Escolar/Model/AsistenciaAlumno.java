package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "asistenciaAlumno")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AsistenciaAlumno {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asistencia_alumno")
    Integer idAsistencia;
    @ManyToOne
    @JoinColumn(name = "id_matricula", nullable = false)
    Matricula matricula;
    @ManyToOne
    @JoinColumn(name = "id_estado", nullable = false)
    EstadoAsistencia estado;
    @ManyToOne
    @JoinColumn(name = "id_justificacion")
    Justificacion justificacion;
    @ManyToOne
    @JoinColumn(name = "id_usuario_registro", nullable = false)
    Usuario usuarioRegistro;
    @Column(name = "fecha", nullable = false)
    LocalDate fecha;
    @Column(name = "hora_entrada", nullable = false)
    LocalTime horaEntrada;
    @Column(name = "hora_salida")
    LocalTime horaSalida;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}