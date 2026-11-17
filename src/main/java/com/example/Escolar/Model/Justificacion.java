package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "justificacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Justificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_justificacion")
    Integer idJustificacion;
    @Column(name = "motivo", length = 150, nullable = false)
    String motivo;
    /** Día en que se creó el motivo; lo pone el sistema (MigracionJustificacion la deja obligatoria). */
    @Column(name = "fecha_registro")
    LocalDate fechaRegistro;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}