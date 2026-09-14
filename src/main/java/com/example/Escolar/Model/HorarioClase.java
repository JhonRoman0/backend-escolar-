package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "HorarioClase")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HorarioClase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idHorario;
    @ManyToOne
    @JoinColumn(name = "idAsignacion", nullable = false)
    Asignacion asignacion;
    @ManyToOne
    @JoinColumn(name = "idAula", nullable = false)
    Aula aula;
    @Column(name = "diaSemana", length = 10, nullable = false)
    byte diaSemana;
    @Column(name = "horaInicio", nullable = false)
    LocalTime horaInicio;
    @Column(name = "horaFin", nullable = false)
    LocalTime horaFin;
    @Column(name = "acceso", length = 10, nullable = false)
    byte acceso;
}
