package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Historial de dinero de una matrícula: nunca se edita ni se reemplaza un movimiento.
@Entity
@Table(name = "pago_matricula")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagoMatricula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago_matricula")
    Integer idPagoMatricula;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_matricula", nullable = false)
    Matricula matricula;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 15, nullable = false)
    TipoPago tipo;
    // Siempre positivo; la devolución resta por su tipo.
    @Column(name = "monto", precision = 10, scale = 2, nullable = false)
    BigDecimal monto;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_metodo_pago", nullable = false)
    MetodoPago metodoPago;
    @Column(name = "codigo_transaccion", length = 60)
    String codigoTransaccion;
    @Column(name = "url_comprobante")
    String urlComprobante;
    @Column(name = "pk_comprobante")
    String pkComprobante;
    @Column(name = "fecha", nullable = false)
    LocalDateTime fecha;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false)
    Usuario usuario;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}
