package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "aula")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Aula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aula")
    Integer idAula;
    @Column(name = "nombre", length = 50, nullable = false, unique = true)
    String nombre;
    @Column(name = "capacidad", nullable = false)
    Integer capacidad;
    // Patio, losa deportiva o auditorio: admite varias clases a la misma hora,
    // asi que no se valida el cruce de aula.
    @Column(name = "uso_compartido", nullable = false, columnDefinition = "bit(1) not null default 0")
    boolean usoCompartido;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}
