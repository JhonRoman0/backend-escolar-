package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "permiso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Permiso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso")
    Integer idPermiso;
    @Column(name = "codigo", length = 50, nullable = false, unique = true)
    String codigo;
    @Column(name = "nombre", length = 60, nullable = false)
    String nombre;
    @ManyToOne
    @JoinColumn(name = "id_modulo", nullable = false)
    Modulo modulo;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}
