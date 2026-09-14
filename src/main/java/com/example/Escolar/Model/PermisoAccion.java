package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PermisoAccion",
        uniqueConstraints = @UniqueConstraint(name = "uk_permiso_accion", columnNames = {"idPermiso", "idAccion"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermisoAccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idPermisoAccion;
    @ManyToOne
    @JoinColumn(name = "idPermiso", nullable = false)
    Permiso permiso;
    @ManyToOne
    @JoinColumn(name = "idAccion", nullable = false)
    Accion accion;
    @Column(name = "acceso", length = 10, nullable = false)
    byte acceso;
}
