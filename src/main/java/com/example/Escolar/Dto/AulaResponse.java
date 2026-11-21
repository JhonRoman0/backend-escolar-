package com.example.Escolar.Dto;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AulaResponse {
    private Integer idAula;
    private String nombre;
    private Integer capacidad;
    private String tipo;
    /** Secciones de las que es salon, de todos los años. */
    private List<SeccionDelAula> secciones;
    private Long accesoId;
    private Boolean tieneAsignacionesVigentes;
    private List<SeccionDelAula> seccionesVigentes = List.of();
    private Map<String, Long> horariosVigentesPorAnio = Map.of();
    private Integer ocupacionActual;

    public record SeccionDelAula(Integer idAnio, String anio, String nivel, String grado, String seccion, String turno) {
    }
}