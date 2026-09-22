package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "horarioClase")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HorarioClase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario_clase")
    Integer idHorario;
    @ManyToOne
    @JoinColumn(name = "id_asignacion", nullable = false)
    Asignacion asignacion;
    @ManyToOne
    @JoinColumn(name = "id_aula", nullable = false)
    Aula aula;
    @Column(name = "dia_semana", length = 10, nullable = false)
    byte diaSemana;
    @Column(name = "hora_inicio", nullable = false)
    LocalTime horaInicio;
    @Column(name = "hora_fin", nullable = false)
    LocalTime horaFin;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}
