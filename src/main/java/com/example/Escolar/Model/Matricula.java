package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "matricula")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Matricula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_matricula")
    Integer idMatricula;
    @ManyToOne
    @JoinColumn(name = "id_alumno_apoderado", nullable = false)
    AlumnoApoderado alumnoApoderado;
    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    Usuario usuario;
    @ManyToOne
    @JoinColumn(name = "id_grado_seccion", nullable = false)
    GradoSeccion gradoSeccion;
    @Column(name = "solicitud_matricula", nullable = false)
    byte solicitudMatricula;
    @Column(name = "fecha_pago")
    LocalDate fechaPago;
    @Column(name = "monto_pago")
    BigDecimal montoPago;
    @Column(name = "fecha_registro", nullable = false)
    LocalDate fechaRegistro;
    @Column(name = "observaciones", length = 500)
    String observaciones;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}