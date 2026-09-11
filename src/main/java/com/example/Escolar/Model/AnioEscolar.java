package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "AnioEscolar")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnioEscolar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idAnio;
    @Column(name = "anio", length = 4, nullable = false, unique = true)
    String anio;
    @Column(name = "estado", length = 10, nullable = false)
    byte estado;
    @Column(name = "acceso", length = 10, nullable = false)
    byte acceso;
}
