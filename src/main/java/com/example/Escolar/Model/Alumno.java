package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "alumno")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Alumno {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id_alumno")
    Integer idAlumno;
    @Column(name = "nombre", length = 30, nullable = false)
    String nombre;
    @Column(name = "apellido_pat", length = 30, nullable = false)
    String apellidoPat;
    @Column(name = "apellido_mat", length = 30, nullable = false)
    String apellidoMat;
    @Column(name = "codigo", length = 16, nullable = false, unique = true)
    String codigo;
    @Column(name = "codigo_hash", length = 64, nullable = false, unique = true)
    String codigoHash;
    @Column(name = "fecha_naci", nullable = false)
    LocalDate fechaNacimiento;
    @Column(name = "direccion", length = 120)
    String direccion;
    @Column(name = "documento_identidad", length = 20, unique = true)
    String documentoIdentidad;
    @Column(name = "url_foto", nullable = true)
    String urlFoto;
    @Column(name = "pk_url_foto", nullable = true)
    String pkUrlFoto;
    @Column(name = "fecha_ingreso", nullable = false)
    LocalDate fechaIngreso;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}