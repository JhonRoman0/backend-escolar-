package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "RolPermiso",
        uniqueConstraints = @UniqueConstraint(name = "uk_rol_permiso", columnNames = {"idRol", "idPermiso"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolPermiso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idRolPermiso;
    @ManyToOne
    @JoinColumn(name = "idRol", nullable = false)
    Rol rol;
    @ManyToOne
    @JoinColumn(name = "idPermiso", nullable = false)
    Permiso permiso;
    @Column(name = "fechaAsignacion", nullable = false)
    LocalDateTime fechaAsignacion;
    @Column(name = "acceso", length = 10, nullable = false)
    byte acceso;
}
