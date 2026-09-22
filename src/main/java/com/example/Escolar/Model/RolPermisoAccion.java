package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rolPermisoAccion",
        uniqueConstraints = @UniqueConstraint(name = "uk_rolpermiso_accion", columnNames = {"id_rol_permiso", "id_accion"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolPermisoAccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol_permiso_accion")
    Integer idRolPermisoAccion;
    @ManyToOne
    @JoinColumn(name = "id_rol_permiso", nullable = false)
    RolPermiso rolPermiso;
    @ManyToOne
    @JoinColumn(name = "id_accion", nullable = false)
    Accion accion;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}
