package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "anioEscolar")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnioEscolar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id_anio")
    Integer idAnio;
    @Column(name = "anio", length = 4, nullable = false, unique = true)
    String anio;
    @Column(name = "estado", length = 10, nullable = false)
    byte estado;
    @Column(name = "fecha_inicio")
    LocalDate fechaInicio;
    @Column(name = "fecha_fin")
    LocalDate fechaFin;
    @Column(name = "bloqueo_horarios_por_fecha")
    Boolean bloqueoHorariosPorFecha;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}
