package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Aviso de falta ya enviado: uno por alumno y día, aunque el backend se reinicie. */
@Entity
@Table(name = "aviso_falta", uniqueConstraints = @UniqueConstraint(name = "uk_aviso_falta", columnNames = {"id_matricula", "fecha"}))
@Getter
@Setter
@NoArgsConstructor
public class AvisoFalta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aviso_falta")
    private Integer idAvisoFalta;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_matricula", nullable = false)
    private Matricula matricula;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;
}
