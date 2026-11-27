package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "cambio_docente_asignacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CambioDocenteAsignacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cambio_docente_asignacion")
    Integer idCambioDocenteAsignacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_cambio", nullable = false)
    CambioDocente cambio;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_asignacion", nullable = false)
    Asignacion asignacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_docente_desde", nullable = false)
    Docente docenteDesde;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_docente_hacia")
    Docente docenteHacia;

    @Column(name = "sin_cobertura", nullable = false)
    Boolean sinCobertura;

    @Column(name = "devuelta", nullable = false)
    Boolean devuelta;

    @Column(name = "fecha_transferencia")
    LocalDate fechaTransferencia;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario_transferencia")
    Usuario usuarioTransferencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_transferencia", length = 15)
    OrigenOperacion origenTransferencia;

    @Column(name = "fecha_devolucion")
    LocalDate fechaDevolucion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario_devolucion")
    Usuario usuarioDevolucion;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_devolucion", length = 15)
    OrigenOperacion origenDevolucion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}
