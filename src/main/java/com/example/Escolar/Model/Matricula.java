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
    // Precio del nivel en ese año; lo pagado está en pago_matricula.
    @Column(name = "monto", precision = 10, scale = 2)
    BigDecimal monto;
    @Column(name = "fecha_registro", nullable = false)
    LocalDate fechaRegistro;
    @Column(name = "observaciones", length = 500)
    String observaciones;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}