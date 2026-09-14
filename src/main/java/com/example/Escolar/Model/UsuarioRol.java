package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "UsuarioRol")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idUsuarioRol;
    @ManyToOne
    @JoinColumn(name = "idUsuario", nullable = false)
    Usuario usuario;
    @ManyToOne
    @JoinColumn(name = "idRol", nullable = false)
    Rol rol;
    @Column(name = "fechaAsignacion", nullable = false)
    LocalDateTime fechaAsignacion;
}
