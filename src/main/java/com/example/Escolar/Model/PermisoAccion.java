package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "permisoAccion",
        uniqueConstraints = @UniqueConstraint(name = "uk_permiso_accion", columnNames = {"id_permiso", "id_accion"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermisoAccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso_accion")
    Integer idPermisoAccion;
    @ManyToOne
    @JoinColumn(name = "id_permiso", nullable = false)
    Permiso permiso;
    @ManyToOne
    @JoinColumn(name = "id_accion", nullable = false)
    Accion accion;
    // Nombre de la accion en este modulo ("Importar, copiar y agregar subcursos").
    // Null: se muestra el nombre generico de la accion.
    @Column(name = "etiqueta", length = 80)
    String etiqueta;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}
