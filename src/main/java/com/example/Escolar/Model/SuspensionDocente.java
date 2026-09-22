package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "suspension_docente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SuspensionDocente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_suspension")
    Integer idSuspension;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_docente", nullable = false)
    Docente docente;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_sustituto")
    Docente sustituto;
    @Column(name = "motivo", length = 30, nullable = false)
    String motivo;
    @Column(name = "motivo_detalle", length = 255)
    String motivoDetalle;
    @Column(name = "fecha_inicio", nullable = false)
    LocalDate fechaInicio;
    @Column(name = "fecha_fin")
    LocalDate fechaFin;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}
