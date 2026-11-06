package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/** Un recreo de un grado en un turno: aplica a todas sus secciones de ese turno. */
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
    @JoinColumn(name = "id_anio", nullable = false)
    AnioEscolar anioEscolar;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_nivel", nullable = false)
    Nivel nivel;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_turno", nullable = false)
    Turno turno;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_grado", nullable = false)
    Grado grado;
    // 1 = lunes ... 6 = sabado. El domingo no hay clases.
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
