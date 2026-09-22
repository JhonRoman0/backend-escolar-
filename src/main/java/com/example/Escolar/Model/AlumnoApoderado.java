package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "alumnoApoderado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlumnoApoderado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id_alumno_apoderado")
    Integer idAlumnoApoderado;
    @ManyToOne
    @JoinColumn(name = "id_alumno", nullable = false)
    Alumno alumno;
    @ManyToOne
    @JoinColumn(name = "id_apoderado", nullable = false)
    Apoderado apoderado;
    @Column(name = "apo_principal", nullable = false)
    byte apoPrincipal;
}