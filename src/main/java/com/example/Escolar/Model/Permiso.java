package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Permiso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Permiso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idPermiso;
    @Column(name = "codigo", length = 50, nullable = false, unique = true)
    String codigo;
    @Column(name = "nombre", length = 60, nullable = false)
    String nombre;
    @ManyToOne
    @JoinColumn(name = "idModulo", nullable = false)
    Modulo modulo;
    @Column(name = "acceso", length = 10, nullable = false)
    byte acceso;
}
