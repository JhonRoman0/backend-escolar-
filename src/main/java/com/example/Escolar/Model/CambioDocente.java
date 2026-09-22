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
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_asignacion", nullable = false)
    Asignacion asignacion;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_docente_anterior", nullable = false)
    Docente docenteAnterior;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_docente_nuevo", nullable = false)
    Docente docenteNuevo;
    @Column(name = "motivo", length = 30, nullable = false)
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
}
