package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Nota final del año de una competencia. Una activa por alumno y competencia;
 * al corregirla o reabrirla, la anterior queda borrada con su motivo.
 */
@Entity
@Table(name = "nota_final_anual")
@Getter
@Setter
@NoArgsConstructor
public class NotaFinalAnual {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nota_anual")
    private Integer idNotaAnual;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_matricula", nullable = false)
    private Matricula matricula;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_competencia", nullable = false)
    private Competencia competencia;

    @Column(name = "calificacion", length = 3, nullable = false)
    private String calificacion;

    /** Solo en escala literal. */
    @Column(name = "conclusion_descriptiva", columnDefinition = "TEXT")
    private String conclusionDescriptiva;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario_registro", nullable = false)
    private Usuario usuarioRegistro;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_acceso", nullable = false)
    private Acceso acceso;

    @Column(name = "motivo_anulacion", length = Nota.MOTIVO_MAXIMO)
    private String motivoAnulacion;

    @ManyToOne
    @JoinColumn(name = "id_usuario_anulacion")
    private Usuario usuarioAnulacion;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;
}
