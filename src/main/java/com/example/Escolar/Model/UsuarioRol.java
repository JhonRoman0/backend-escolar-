package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarioRol")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario_rol")
    Integer idUsuarioRol;
    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    Usuario usuario;
    @ManyToOne
    @JoinColumn(name = "id_rol", nullable = false)
    Rol rol;
    @Column(name = "fecha_asignacion", nullable = false)
    LocalDateTime fechaAsignacion;
}
