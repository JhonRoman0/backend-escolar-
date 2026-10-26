package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "metodo_pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MetodoPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_metodo_pago")
    Integer idMetodoPago;
    @Column(name = "nombre", length = 40, nullable = false)
    String nombre;
    @Column(name = "requiere_codigo", nullable = false)
    boolean requiereCodigo;
    @Column(name = "requiere_comprobante", nullable = false)
    boolean requiereComprobante;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}
