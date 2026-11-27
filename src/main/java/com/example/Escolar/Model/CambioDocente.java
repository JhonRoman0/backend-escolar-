package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "cambio_docente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CambioDocente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cambio")
    Integer idCambio;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_suspension")
    SuspensionDocente suspension;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_docente_anterior", nullable = false)
    Docente docenteAnterior;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_docente_nuevo")
    Docente docenteNuevo;

    @Column(name = "motivo", length = 150, nullable = false)
    String motivo;

    @Column(name = "motivo_detalle", length = 255)
    String motivoDetalle;

    @Column(name = "fecha_cambio", nullable = false)
    LocalDate fechaCambio;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario_registro", nullable = false)
    Usuario usuarioRegistro;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 15, nullable = true)
    TipoCambioDocente tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 15, nullable = true)
    EstadoCambioDocente estado;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_anio", nullable = true)
    AnioEscolar anioEscolar;

    @Column(name = "fecha_inicio", nullable = true)
    LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    LocalDate fechaFin;

    @Column(name = "fecha_aplicacion")
    LocalDate fechaAplicacion;

    @Column(name = "fecha_finalizacion")
    LocalDate fechaFinalizacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_motivo", nullable = true)
    MotivoCambioDocente motivoRelacionado;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_registro", length = 15, nullable = true)
    OrigenOperacion origenRegistro;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_ultima_operacion", length = 15, nullable = true)
    OrigenOperacion origenUltimaOperacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario_ultima_operacion")
    Usuario usuarioUltimaOperacion;

    @Column(name = "motivo_resolucion", length = 255)
    String motivoResolucion;

    @Column(name = "incidencia", nullable = true)
    Boolean incidencia;

    @Column(name = "motivo_incidencia", length = 255)
    String motivoIncidencia;

    @Column(name = "resolucion_administrativa", length = 500)
    String resolucionAdministrativa;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario_resolucion")
    Usuario usuarioResolucion;

    @Column(name = "fecha_resolucion")
    LocalDate fechaResolucion;
}
