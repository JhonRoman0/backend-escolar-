package com.example.Escolar.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "plantillaSiagie")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaSiagie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plantilla_siagie")
    Integer idPlantilla;

    @Column(name = "anio", length = 10, nullable = false)
    String anio;

    @Column(name = "vigente", nullable = false)
    byte vigente;

    @Lob
    @Column(name = "archivo", nullable = false)
    byte[] archivo;

    @Column(name = "nombre_archivo", length = 120, nullable = false)
    String nombreArchivo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_acceso", nullable = false)
    Acceso acceso;
}
