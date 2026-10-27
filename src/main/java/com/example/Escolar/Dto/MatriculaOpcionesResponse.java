package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/** Grados y secciones en los que se puede matricular al alumno en un año. */
@Getter
@Setter
public class MatriculaOpcionesResponse {
    /** Motivo por el que no se puede matricular en ese año; si viene, grados está vacío. */
    private String bloqueo;
    /** Ej. "1ro Primaria 2026"; null para un alumno nuevo. */
    private String ultimoGrado;
    private List<GradoOpcion> grados;

    @Getter
    @Setter
    public static class GradoOpcion {
        private Integer idGrado;
        private String grado;
        private Integer idNivel;
        private String nivel;
        /** null: el monto de ese nivel y año no está configurado. */
        private BigDecimal monto;
        /** El grado siguiente, que el formulario preselecciona. */
        private boolean sugerido;
        private List<SeccionOpcion> secciones;
    }

    @Getter
    @Setter
    public static class SeccionOpcion {
        private Integer idGradoSeccion;
        private String seccion;
        private Integer idTurno;
        private String turno;
        private Integer capacidad;
        private long ocupados;
    }
}
