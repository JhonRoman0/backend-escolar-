package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "recreo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Recreo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recreo")
    Integer idRecreo;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_nivel", nullable = false)
    Nivel nivel;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_grado_seccion")
    GradoSeccion gradoSeccion;
    @Column(name = "dia_semana", nullable = false)
    Byte diaSemana;
    @Column(name = "hora_inicio", nullable = false)
    LocalTime horaInicio;
    @Column(name = "hora_fin", nullable = false)
    LocalTime horaFin;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}
