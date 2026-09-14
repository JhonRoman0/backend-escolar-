package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Ajuste_Portal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AjustePortal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idAjuste;
    @Column(name = "clave", length = 60, nullable = false, unique = true)
    String clave;
    @Column(name = "valor", length = 255, nullable = false)
    String valor;
    @Column(name = "acceso", nullable = false)
    byte acceso = 1;
}