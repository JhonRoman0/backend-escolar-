package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Bimestre o trimestre de un nivel en un año. No guarda estado: abierto o
 * cerrado sale de sus fechas, igual que el año escolar se cierra solo.
 */
@Entity
@Table(name = "periodo_evaluacion",
        uniqueConstraints = @UniqueConstraint(name = "uk_periodo_anio_nivel_numero",
                columnNames = {"id_anio", "id_nivel", "numero"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PeriodoEvaluacion {
    public static final String POR_COMENZAR = "POR_COMENZAR";
    public static final String ABIERTO = "ABIERTO";
    public static final String CERRADO = "CERRADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_periodo")
    Integer idPeriodo;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_anio", nullable = false)
    AnioEscolar anioEscolar;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_nivel", nullable = false)
    Nivel nivel;
    @Column(name = "numero", nullable = false)
    byte numero;
    @Column(name = "fecha_inicio", nullable = false)
    LocalDate fechaInicio;
    @Column(name = "fecha_fin", nullable = false)
    LocalDate fechaFin;

    public static final java.time.format.DateTimeFormatter FECHA = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String estadoEn(LocalDate dia) {
        if (dia.isBefore(fechaInicio)) {
            return POR_COMENZAR;
        }
        return dia.isAfter(fechaFin) ? CERRADO : ABIERTO;
    }
}
