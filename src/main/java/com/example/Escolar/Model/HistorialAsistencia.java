package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "HistorialAsistencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistorialAsistencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idHistorial;
    @ManyToOne
    @JoinColumn(name = "idAsistenciaAlumno", nullable = false)
    AsistenciaAlumno asistenciaAlumno;
    @ManyToOne
    @JoinColumn(name = "idUsuario", nullable = false)
    Usuario usuario;
    @Column(name = "fechaHora", nullable = false)
    LocalDateTime fechaHora;
    @Column(name = "accion", length = 30, nullable = false)
    String accion;
    @Column(name = "estadoAnterior", length = 30)
    String estadoAnterior;
    @Column(name = "estadoNuevo", length = 30)
    String estadoNuevo;
    @Column(name = "detalle", length = 250)
    String detalle;
    @Column(name = "acceso", nullable = false)
    byte acceso = 1;
}