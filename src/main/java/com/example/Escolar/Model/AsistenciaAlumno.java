package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
// Las marcas siempre se buscan por matrícula y rango de fechas (día, semana, mes):
// sin este índice se leían todas las del año de cada matrícula.
@Table(name = "asistenciaAlumno", indexes = @Index(name = "idx_asistencia_matricula_fecha", columnList = "id_matricula, fecha"))

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
    // Ingreso justificado (pasado el límite de tardanza): quién lo trajo y la foto de evidencia.
    @Column(name = "acompanante", length = 150)
    String acompanante;
    @Column(name = "url_evidencia", length = 500)
    String urlEvidencia;
    @Column(name = "pk_evidencia")
    String pkEvidencia;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}