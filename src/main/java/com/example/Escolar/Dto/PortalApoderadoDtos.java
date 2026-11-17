package com.example.Escolar.Dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Respuestas del portal del apoderado (solo lectura, sus hijos). */
public final class PortalApoderadoDtos {

    private PortalApoderadoDtos() {
    }

    public record Hijo(Integer idAlumno, String nombre, String urlFoto, List<MatriculaHijo> matriculas) {
    }

    /** vigente: la del año escolar en curso (la que se muestra por defecto). */
    public record MatriculaHijo(Integer idMatricula, Integer idAnio, String anio, boolean vigente,
                                String nivel, String grado, String seccion, String turno) {
    }

    /**
     * estado null: día futuro. "Sin clases": feriado, vacaciones o fin de semana.
     * urlEvidencia: foto del ingreso justificado.
     */
    public record DiaAsistencia(LocalDate fecha, String estado, LocalTime horaEntrada,
                                String justificacion, String acompanante, String urlEvidencia) {
    }

    public record AsistenciaMes(int anio, int mes, long puntuales, long tardanzas, long justificadas,
                                long faltas, List<DiaAsistencia> dias) {
    }

    /**
     * calificacion null: el docente aún no la registra. otraSeccion: la sección
     * anterior del hijo (cambio de sección) donde se registró; null si es la actual.
     */
    public record NotaDeEvaluacion(String nombre, LocalDate fecha, String calificacion, String otraSeccion) {
    }

    public record PeriodoNotas(int numero, String nombre, String estado, List<NotaDeEvaluacion> evaluaciones) {
    }

    public record CursoNotas(String curso, List<PeriodoNotas> periodos) {
    }

    public record UltimaNota(String curso, String evaluacion, LocalDate fecha, String calificacion,
                             LocalDateTime registrada) {
    }

    public record Resumen(AsistenciaMes asistencia, List<UltimaNota> ultimasNotas,
                          List<HorarioPlanoResponse> clasesDeHoy) {
    }

    public record Pago(LocalDateTime fecha, String tipo, BigDecimal monto, String metodo, String urlComprobante) {
    }

    /** monto/saldo null: la matrícula no tiene monto registrado. */
    public record Pagos(BigDecimal monto, BigDecimal pagado, BigDecimal saldo, List<Pago> pagos) {
    }
}
