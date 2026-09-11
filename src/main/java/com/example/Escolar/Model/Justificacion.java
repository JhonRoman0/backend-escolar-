package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "Justificacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Justificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idJustificacion;
    @Column(name = "motivo", length = 150, nullable = false)
    String motivo;
    @Column(name = "documentoUrl")
    String documentoUrl;
    @Column(name = "fechaJustificacion", nullable = false)
    LocalDate fechaJustificacion;
    @Column(name = "acceso", nullable = false)
    byte acceso = 1;
}