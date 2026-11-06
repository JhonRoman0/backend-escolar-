package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "gradoSeccion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GradoSeccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_grado_seccion")
    Integer idGradoSeccion;
    @ManyToOne
    @JoinColumn(name = "id_grado", nullable = false)
    Grado grado;
    @ManyToOne
    @JoinColumn(name = "id_seccion", nullable = true)
    Seccion seccion;
    @ManyToOne
    @JoinColumn(name = "id_turno", nullable = false)
    Turno turno;
    @ManyToOne
    @JoinColumn(name = "id_anio", nullable = false)
    AnioEscolar anioEscolar;
    // Salon fijo de la seccion (aula de tipo SALON, una por seccion por año y
    // turno). Opcional en la base para las secciones anteriores a la regla; las
    // pantallas lo piden siempre y sin el no se le asignan cursos.
    @ManyToOne
    @JoinColumn(name = "id_aula")
    Aula aula;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}