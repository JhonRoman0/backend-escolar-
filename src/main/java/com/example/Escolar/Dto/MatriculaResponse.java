package com.example.Escolar.Dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class MatriculaResponse {
    private Integer idMatricula;
    private Integer idAlumno;
    private Integer idAlumnoApoderado;
    private String codigoAlumno;
    private String documentoAlumno;
    private String alumno;
    private String apoderado;
    private Integer idUsuario;
    private String usuarioRegistro;
    private Integer idGradoSeccion;
    private Integer idNivel;
    private String nivel;
    private Integer idGrado;
    private String grado;
    private String seccion;
    private Integer idTurno;
    private String turno;
    private Integer idAnio;
    private String anio;
    /** Estado del año: 1 vigente, 2 cerrado, 3 por comenzar. Define qué se puede editar. */
    private Byte estadoAnio;
    /** Precio del nivel en ese año. */
    private BigDecimal monto;
    /** Pagos y diferencias menos devoluciones. */
    private BigDecimal pagadoNeto;
    private String metodoPago;
    private LocalDate fechaRegistro;
    private String observaciones;
    private Long accesoId;
    private List<HistorialResponse> historial;
}
