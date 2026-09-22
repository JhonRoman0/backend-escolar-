package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "historialAsistencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistorialAsistencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial_asistencia")
    Integer idHistorial;
    @ManyToOne
    @JoinColumn(name = "id_asistencia_alumno", nullable = false)
    AsistenciaAlumno asistenciaAlumno;
    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    Usuario usuario;
    @Column(name = "fecha_hora", nullable = false)
    LocalDateTime fechaHora;
    @Column(name = "accion", length = 30, nullable = false)
    String accion;
    @Column(name = "estado_anterior", length = 30)
    String estadoAnterior;
    @Column(name = "estado_nuevo", length = 30)
    String estadoNuevo;
    @Column(name = "detalle", length = 250)
    String detalle;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}