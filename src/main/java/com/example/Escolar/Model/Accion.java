package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "accion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Accion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id_accion")
    Integer idAccion;
    @Column(name = "codigo", length = 50, nullable = false, unique = true)
    String codigo;
    @Column(name = "nombre", length = 60, nullable = false)
    String nombre;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}
