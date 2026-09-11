package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "Docente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Docente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idDocente;
    @OneToOne
    @JoinColumn(name = "idUsuario", nullable = false)
    Usuario usuario;
    @Column(name = "tipoContrato", length = 30)
    String tipoContrato;
    @Column(name = "fechaContratacion")
    LocalDate fechaContratacion;
    @Column(name = "especialidad", length = 60)
    String especialidad;
    @Column(name = "gradoAcademico", length = 60)
    String gradoAcademico;
    @Column(name = "acceso", nullable = false)
    byte acceso = 1;
}