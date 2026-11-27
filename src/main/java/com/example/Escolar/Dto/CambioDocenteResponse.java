package com.example.Escolar.Dto;

import com.example.Escolar.Model.EstadoCambioDocente;
import com.example.Escolar.Model.OrigenOperacion;
import com.example.Escolar.Model.TipoCambioDocente;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class CambioDocenteResponse {
    private Integer idCambio;
    private TipoCambioDocente tipo;
    private EstadoCambioDocente estado;
    private Integer idAnio;
    private String anio;
    private LocalDate fechaCambio;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private LocalDate fechaAplicacion;
    private LocalDate fechaFinalizacion;
    private Integer idMotivo;
    private String motivo;
    private String motivoDetalle;
    private Integer idDocenteAnterior;
    private String docenteAnterior;
    private Integer idDocenteNuevo;
    private String docenteNuevo;
    private Integer idUsuarioRegistro;
    private String usuarioRegistro;
    private OrigenOperacion origenRegistro;
    private OrigenOperacion origenUltimaOperacion;
    private Integer idUsuarioUltimaOperacion;
    private Boolean incidencia;
    private String motivoIncidencia;
    private String resolucionAdministrativa;
    private LocalDate fechaResolucion;
    private String motivoResolucion;
    private Long accesoId;
    private List<Detalle> detalles;

    public record HorarioResumen(
            Integer idHorario,
            Integer diaSemana,
            String horaInicio,
            String horaFin
    ) {
    }

    public record Detalle(
            Integer idAsignacion,
            Integer idDocenteDesde,
            Integer idDocenteHacia,
            Boolean sinCobertura,
            Boolean devuelta,
            LocalDate fechaTransferencia,
            LocalDate fechaDevolucion,
            OrigenOperacion origenTransferencia,
            OrigenOperacion origenDevolucion,
            String curso,
            String grado,
            String seccion,
            String anio,
            List<HorarioResumen> horarios
    ) {
    }
}
