package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "RolPermisoAccion",
        uniqueConstraints = @UniqueConstraint(name = "uk_rolpermiso_accion", columnNames = {"idRolPermiso", "idAccion"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolPermisoAccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idRolPermisoAccion;
    @ManyToOne
    @JoinColumn(name = "idRolPermiso", nullable = false)
    RolPermiso rolPermiso;
    @ManyToOne
    @JoinColumn(name = "idAccion", nullable = false)
    Accion accion;
    @Column(name = "acceso", length = 10, nullable = false)
    byte acceso;
}
