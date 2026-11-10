package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/** Procedencia de una nota reconocida. Las anulaciones conservan esta fila. */
@Entity
@Table(name = "reconocimiento_calificacion", check = {
        @CheckConstraint(name = "ck_reconocimiento_distinto", constraint = "id_nota_origen <> id_nota_destino"),
        @CheckConstraint(name = "ck_reconocimiento_estado", constraint = "(vigencia is not null and vigencia = 1 and id_usuario_anulacion is null and fecha_anulacion is null and motivo_anulacion is null) or (vigencia is null and id_usuario_anulacion is not null and fecha_anulacion is not null and motivo_anulacion is not null and char_length(trim(motivo_anulacion)) > 0)")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_reconocimiento_origen_seccion", columnNames = {"id_nota_origen", "id_seccion_destino", "vigencia"}),
        @UniqueConstraint(name = "uk_reconocimiento_destino", columnNames = {"id_nota_destino", "vigencia"})
})
@Getter
@Setter
@NoArgsConstructor
public class ReconocimientoCalificacion {
    public static final byte VIGENTE = 1;
    public static final String ACTIVO = "ACTIVO";
    public static final String ANULADO = "ANULADO";
    public static final int MOTIVO_MAXIMO = 500;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reconocimiento")
    private Integer idReconocimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_nota_origen", nullable = false)
    private NotaEvaluacion notaOrigen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_nota_destino", nullable = false)
    private NotaEvaluacion notaDestino;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_seccion_destino", nullable = false)
    private GradoSeccion seccionDestino;

    @Column(name = "calificacion", nullable = false, length = 3)
    private String calificacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "escala", nullable = false, length = 15)
    private EscalaCalificacion escala;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_registro", nullable = false)
    private Usuario usuarioRegistro;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    /** 1 activo; null anulado, para conservar auditoria sin ocupar el indice unico. */
    @Column(name = "vigencia")
    private Byte vigencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_anulacion")
    private Usuario usuarioAnulacion;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "motivo_anulacion", length = MOTIVO_MAXIMO)
    private String motivoAnulacion;

    @PrePersist
    @PreUpdate
    private void validarEstado() {
        boolean activo = Byte.valueOf(VIGENTE).equals(vigencia);
        boolean anulado = vigencia == null && usuarioAnulacion != null && fechaAnulacion != null
                && motivoAnulacion != null && !motivoAnulacion.isBlank();
        if ((!activo && !anulado) || (activo && (usuarioAnulacion != null || fechaAnulacion != null || motivoAnulacion != null))) {
            throw new IllegalStateException("Estado de reconocimiento incoherente");
        }
    }
}
