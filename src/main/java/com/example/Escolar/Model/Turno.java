package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "Turno")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Turno {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idTurno;
    @Column(name = "nombre", length = 50, nullable = false, unique = true)
    String nombre;
    @Column(name = "horaEntrada", nullable = false)
    LocalTime horaEntrada;
    @Column(name = "horaEntradaLimite", nullable = false)
    LocalTime horaEntradaLimite;
    @Column(name = "horaFaltaLimite", nullable = false)
    LocalTime horaFaltaLimite;
    @Column(name = "horaSalida", nullable = false)
    LocalTime horaSalida;
    @Column(name = "acceso", length = 10, nullable = false)
    byte acceso;
}