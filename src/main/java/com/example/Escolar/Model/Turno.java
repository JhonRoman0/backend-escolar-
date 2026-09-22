package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "turno")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Turno {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_turno")
    Integer idTurno;
    @Column(name = "nombre", length = 50, nullable = false, unique = true)
    String nombre;
    @Column(name = "hora_entrada", nullable = false)
    LocalTime horaEntrada;
    @Column(name = "hora_entrada_limite", nullable = false)
    LocalTime horaEntradaLimite;
    @Column(name = "hora_falta_limite", nullable = false)
    LocalTime horaFaltaLimite;
    @Column(name = "hora_salida", nullable = false)
    LocalTime horaSalida;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}