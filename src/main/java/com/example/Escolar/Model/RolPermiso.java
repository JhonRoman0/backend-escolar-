package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "rolPermiso",
        uniqueConstraints = @UniqueConstraint(name = "uk_rol_permiso", columnNames = {"id_rol", "id_permiso"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolPermiso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol_permiso")
    Integer idRolPermiso;
    @ManyToOne
    @JoinColumn(name = "id_rol", nullable = false)
    Rol rol;
    @ManyToOne
    @JoinColumn(name = "id_permiso", nullable = false)
    Permiso permiso;
    @Column(name = "fecha_asignacion", nullable = false)
    LocalDateTime fechaAsignacion;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}
