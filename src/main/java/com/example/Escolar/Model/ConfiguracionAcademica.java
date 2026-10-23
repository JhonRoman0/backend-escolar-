package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Reglas academicas de un nivel en un año escolar. Por ahora solo la duracion
 * de la hora academica; mas adelante tambien el sistema de calificacion.
 */
@Entity
@Table(name = "configuracion_academica",
        uniqueConstraints = @UniqueConstraint(name = "uk_configuracion_anio_nivel", columnNames = {"id_anio", "id_nivel"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionAcademica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_configuracion")
    Integer idConfiguracion;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_anio", nullable = false)
    AnioEscolar anioEscolar;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_nivel", nullable = false)
    Nivel nivel;
    // Minutos de la hora academica (30 a 60, de 5 en 5). Null: libre, la clase
    // dura lo que se elija (inicial se organiza por actividades).
    @Column(name = "minutos_hora_academica")
    Integer minutosHoraAcademica;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}
