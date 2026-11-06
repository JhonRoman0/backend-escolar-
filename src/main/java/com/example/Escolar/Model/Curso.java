package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "curso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Curso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_curso")
    Integer idCurso;
    // Codigo MINEDU tal como lo trae el SIAGIE ("063: MATE"). Los subcursos no tienen.
    @Column(name = "codigo", length = 50)
    String codigo;
    @Column(name = "nombre", length = 150, nullable = false)
    String nombre;
    // Cada año escolar y nivel tiene su propia malla: asi el historial no cambia
    // cuando se configura el año siguiente.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_anio", nullable = false)
    AnioEscolar anioEscolar;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_nivel", nullable = false)
    Nivel nivel;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 15, nullable = false)
    TipoCurso tipo;
    @Column(name = "tiene_subcursos", nullable = false)
    boolean tieneSubcursos;
    @ManyToOne
    @JoinColumn(name = "id_curso_padre")
    Curso cursoPadre;
    // Solo para subcursos: las competencias del area que evalua.
    @ManyToMany
    @JoinTable(name = "subcurso_competencia",
            joinColumns = @JoinColumn(name = "id_curso"),
            inverseJoinColumns = @JoinColumn(name = "id_competencia"))
    Set<Competencia> competencias = new HashSet<>();
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}
