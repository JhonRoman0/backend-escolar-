package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "motivo_cambio_docente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MotivoCambioDocente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_motivo_cambio_docente")
    Integer idMotivoCambioDocente;

    @Column(name = "nombre", length = 150, nullable = false, unique = true)
    String nombre;

    @Column(name = "aplica_temporal", nullable = false)
    Boolean aplicaTemporal;

    @Column(name = "aplica_definitivo", nullable = false)
    Boolean aplicaDefinitivo;

    @Column(name = "fecha_registro", nullable = false)
    LocalDate fechaRegistro;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}
