package com.example.Escolar.Exception;

import com.example.Escolar.Service.VigenciaService.EstadoVigencia;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class VigenciaNoValidaException extends RuntimeException {

    public static final String CODIGO_VIGENCIA_NO_INICIADA = "VIGENCIA_NO_INICIADA";
    public static final String CODIGO_VIGENCIA_VENCIDA = "VIGENCIA_VENCIDA";
    public static final String CODIGO_TOKEN_OBSOLETO = "VIGENCIA_TOKEN_OBSOLETO";
    public static final String CODIGO_NO_DISPONIBLE = "VIGENCIA_NO_DISPONIBLE";
    public static final String CODIGO_CUENTA_INACTIVA = "VIGENCIA_CUENTA_INACTIVA";
    public static final String CODIGO_ROL_NO_DISPONIBLE = "VIGENCIA_ROL_NO_DISPONIBLE";
    public static final String MENSAJE_CUENTA_INACTIVA =
            "Su cuenta está inactiva. Comuníquese con el administrador.";
    public static final String MENSAJE_NO_DISPONIBLE = "No se pudo verificar la vigencia de su cuenta.";
    public static final String MENSAJE_ROL_NO_DISPONIBLE =
            "El rol activo de su sesión ya no está disponible. Vuelva a iniciar sesión.";

    private final String codigo;
    private final LocalDate fechaInicio;
    private final LocalDate fechaFin;
    private final EstadoVigencia estado;

    public VigenciaNoValidaException(EstadoVigencia estado, LocalDate fechaInicio, LocalDate fechaFin) {
        super(mensaje(estado));
        this.estado = estado;
        this.codigo = codigo(estado);
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    public VigenciaNoValidaException(String codigo, String message) {
        super(message);
        this.codigo = codigo;
        this.fechaInicio = null;
        this.fechaFin = null;
        this.estado = null;
    }

    private static String codigo(EstadoVigencia estado) {
        if (estado == EstadoVigencia.NO_INICIADA) {
            return CODIGO_VIGENCIA_NO_INICIADA;
        }
        if (estado == EstadoVigencia.VENCIDA) {
            return CODIGO_VIGENCIA_VENCIDA;
        }
        return CODIGO_NO_DISPONIBLE;
    }

    private static String mensaje(EstadoVigencia estado) {
        if (estado == EstadoVigencia.NO_INICIADA) {
            return "El período de vigencia de su cuenta aún no ha comenzado.";
        }
        if (estado == EstadoVigencia.VENCIDA) {
            return "El período de vigencia de su cuenta ha finalizado. Comuníquese con el administrador.";
        }
        return MENSAJE_NO_DISPONIBLE;
    }
}
