package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PlantillaSiagie")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaSiagie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer idPlantilla;

    @Column(name = "anio", length = 10, nullable = false)
    String anio;

    @Column(name = "vigente", nullable = false)
    byte vigente;

    @Lob
    @Column(name = "archivo", nullable = false)
    byte[] archivo;

    @Column(name = "nombreArchivo", length = 120, nullable = false)
    String nombreArchivo;

    @Column(name = "acceso", length = 10, nullable = false)
    byte acceso;
}
