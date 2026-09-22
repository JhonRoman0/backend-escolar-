package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ajustePortal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AjustePortal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id_ajuste_portal")
    Integer idAjuste;
    @Column(name = "clave", length = 60, nullable = false, unique = true)
    String clave;
    @Column(name = "valor", length = 255, nullable = false)
    String valor;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}