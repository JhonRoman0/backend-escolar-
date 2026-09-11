package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "DiaFeriado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DiaFeriado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idDiaFeriado;
    @Column(name = "fecha", nullable = false)
    LocalDate fecha;
    @Column(name = "motivo", length = 150, nullable = false)
    String motivo;
    @ManyToOne
    @JoinColumn(name = "idAnioEscolar")
    AnioEscolar anioEscolar;
    @Column(name = "acceso", nullable = false)
    byte acceso = 1;
}