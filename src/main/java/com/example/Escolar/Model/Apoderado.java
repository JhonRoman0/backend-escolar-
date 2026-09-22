package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "apoderado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Apoderado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_apoderado")
    Integer idApoderado;
    @OneToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    Usuario usuario;
    @Column(name = "celular", length = 15)
    String celular;
    @Column(name = "direccion", length = 120)
    String direccion;
    @Column(name = "parentesco", length = 30)
    String parentesco;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}