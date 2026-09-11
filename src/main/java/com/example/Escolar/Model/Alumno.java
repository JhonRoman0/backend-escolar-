package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "Alumno")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Alumno {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idAlumno;
    @Column(name = "nombre", length = 30, nullable = false)
    String nombre;
    @Column(name = "apellidoPat", length = 30, nullable = false)
    String apellidoPat;
    @Column(name = "apellidoMat", length = 30, nullable = false)
    String apellidoMat;
    @Column(name = "codigo", length = 10, nullable = false, unique = true)
    String codigo;
    @Column(name = "codigoHash", length = 64, nullable = false, unique = true)
    String codigoHash;
    @Column(name = "fechaNacimiento", nullable = false)
    LocalDate fechaNacimiento;
    @Column(name = "direccion", length = 120)
    String direccion;
    @Column(name = "documentoIdentidad", length = 20, unique = true)
    String documentoIdentidad;
    @Column(name = "urlFoto", nullable = true)
    String urlFoto;
    @Column(name = "pkUrlFoto", nullable = true)
    String pkUrlFoto;
    @Column(name = "fechaIngreso", nullable = false)
    LocalDate fechaIngreso;
    @Column(name = "acceso", nullable = false)
    byte acceso = 1;
}