package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "docente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Docente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_docente")
    Integer idDocente;
    @OneToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    Usuario usuario;
    @Column(name = "tipo_contrato", length = 30)
    String tipoContrato;
    @Column(name = "fecha_contratacion")
    LocalDate fechaContratacion;
    @Column(name = "especialidad", length = 60)
    String especialidad;
    @Column(name = "grado_academico", length = 60)
    String gradoAcademico;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}