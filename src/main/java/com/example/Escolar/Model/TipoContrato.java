package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tipo_contrato")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoContrato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_contrato")
    Integer idTipoContrato;
    @Column(name = "nombre", length = 50, nullable = false, unique = true)
    String nombre;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}
