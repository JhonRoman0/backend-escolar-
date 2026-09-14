package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Modulo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Modulo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idModulo;
    @Column(name = "modulo", length = 50, nullable = false)
    String modulo;
    @Column(name = "icono", length = 50)
    String icono;
    @Column(name = "acceso", length = 10, nullable = false)
    byte acceso;
}
