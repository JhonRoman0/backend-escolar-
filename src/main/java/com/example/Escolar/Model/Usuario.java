package com.example.Escolar.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "Usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idUsuario;
    @Column(name = "nombre",length =30,nullable = false)
    String nombre;
    @Column(name = "apellidoPat",length = 30,nullable = false)
    String apellidoPat;
    @Column(name = "apellidoMat", length = 30,nullable = false)
    String apellidoMat;
    @Column(name = "codigo",length = 10,nullable = false)
    String codigo;
    @Column(name = "documentoIdentidad", length = 20, unique = true)
    String documentoIdentidad;
    @Column(name = "contraseña",nullable = false)
    String contraseña;
    @Column(name = "acceso",length = 1,nullable = false)
    byte acceso;
    @Column(name = "gmail",length = 60,nullable = true)
    @Email
    String gmail;
    @Column(name = "fechaNaci",nullable = false)
    LocalDate fechaNaci;
    @Column(name = "urlFoto",nullable = true)
    String urlFoto;
    @Column(name = "pkUrlFoto",nullable = true)
    String pkUrlFoto;
    @Column(name = "fechaCreacion", nullable = false)
    LocalDate fechaCreacion;

}
