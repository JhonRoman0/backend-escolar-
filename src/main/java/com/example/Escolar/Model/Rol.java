package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Rol")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Rol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idRol;
    @Column(name = "nombre",length = 20,nullable = false)
    String nombre;
    @Column(name = "acceso",length = 10,nullable = false)
    byte acceso;
}
