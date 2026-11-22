package com.example.Escolar.Service;

import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Util.FechaColegio;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class VigenciaService {

    public enum EstadoVigencia {
        VIGENTE,
        INDEFINIDA,
        NO_INICIADA,
        VENCIDA
    }

    public EstadoVigencia evaluar(LocalDate fechaInicio, LocalDate fechaFin) {
        return evaluar(fechaInicio, fechaFin, hoy());
    }

    public EstadoVigencia evaluar(LocalDate fechaInicio, LocalDate fechaFin, LocalDate hoy) {
        if (datosNoDisponibles(fechaInicio, fechaFin)) {
            return EstadoVigencia.VENCIDA;
        }
        if (hoy.isBefore(fechaInicio)) {
            return EstadoVigencia.NO_INICIADA;
        }
        if (fechaFin == null) {
            return EstadoVigencia.INDEFINIDA;
        }
        return hoy.isAfter(fechaFin) ? EstadoVigencia.VENCIDA : EstadoVigencia.VIGENTE;
    }

    public boolean datosNoDisponibles(LocalDate fechaInicio, LocalDate fechaFin) {
        return fechaInicio == null || (fechaFin != null && fechaFin.isBefore(fechaInicio));
    }

    public boolean esValida(EstadoVigencia estado) {
        return estado == EstadoVigencia.VIGENTE || estado == EstadoVigencia.INDEFINIDA;
    }

    public boolean esValida(LocalDate fechaInicio, LocalDate fechaFin) {
        return esValida(evaluar(fechaInicio, fechaFin));
    }

    public boolean esAdministrable(Usuario usuario) {
        if (usuario == null || usuario.getAcceso() == null
                || !AccesoConstants.ACTIVO.equals(usuario.getAcceso().getIdAcceso())) {
            return false;
        }
        return esValida(usuario.getFechaInicioVigencia(), usuario.getFechaFinVigencia());
    }

    public LocalDate hoy() {
        return FechaColegio.hoy();
    }
}
