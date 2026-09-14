package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Apoderado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Apoderado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idApoderado;
    @OneToOne
    @JoinColumn(name = "idUsuario", nullable = false)
    Usuario usuario;
    @Column(name = "celular", length = 15)
    String celular;
    @Column(name = "direccion", length = 120)
    String direccion;
    @Column(name = "parentesco", length = 30)
    String parentesco;
    @Column(name = "acceso", nullable = false)
    byte acceso = 1;
}