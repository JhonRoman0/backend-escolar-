package com.example.Escolar.Service;

import com.example.Escolar.Model.Acceso;
import com.example.Escolar.Model.Usuario;
import com.example.Escolar.Service.VigenciaService.EstadoVigencia;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VigenciaServiceTest {

    private final VigenciaService service = new VigenciaService();
    private final LocalDate hoy = LocalDate.of(2026, 11, 20);

    @Test
    void inicioIgualAHoySinFinEsIndefinida() {
        assertEquals(EstadoVigencia.INDEFINIDA, service.evaluar(hoy, null, hoy));
        assertTrue(service.esValida(hoy, null));
    }

    @Test
    void inicioPosteriorAHoyEsNoIniciada() {
        assertEquals(EstadoVigencia.NO_INICIADA, service.evaluar(hoy.plusDays(1), null, hoy));
        assertFalse(service.esValida(service.evaluar(hoy.plusDays(1), null, hoy)));
    }

    @Test
    void finIgualAHoySigueSiendoVigente() {
        assertEquals(EstadoVigencia.VIGENTE, service.evaluar(hoy.minusDays(5), hoy, hoy));
        assertTrue(service.esValida(service.evaluar(hoy.minusDays(5), hoy, hoy)));
    }

    @Test
    void finAnteriorAHoyEsVencida() {
        assertEquals(EstadoVigencia.VENCIDA, service.evaluar(hoy.minusDays(10), hoy.minusDays(1), hoy));
        assertFalse(service.esValida(hoy.minusDays(10), hoy.minusDays(1)));
    }

    @Test
    void aceptaHoyDeLimaInyectadoParaElCambioDeDia() {
        LocalDate inicio = LocalDate.of(2026, 11, 21);
        assertEquals(EstadoVigencia.NO_INICIADA, service.evaluar(inicio, null, LocalDate.of(2026, 11, 20)));
        assertEquals(EstadoVigencia.INDEFINIDA, service.evaluar(inicio, null, LocalDate.of(2026, 11, 21)));
    }

    @Test
    void inicioNuloORangoInvalidoFallaCerradoComoVencida() {
        assertEquals(EstadoVigencia.VENCIDA, service.evaluar(null, null, hoy));
        assertEquals(EstadoVigencia.VENCIDA, service.evaluar(hoy, hoy.minusDays(1), hoy));
        assertFalse(service.esValida(null, null));
    }

    @Test
    void datosNoDisponiblesDetectaFechasNulasOInvertidas() {
        assertTrue(service.datosNoDisponibles(null, null));
        assertTrue(service.datosNoDisponibles(hoy, hoy.minusDays(1)));
        assertFalse(service.datosNoDisponibles(hoy, null));
    }

    @Test
    void esAdministrableExigeAccesoActivoYVigenciaValida() {
        Usuario usuario = new Usuario();
        usuario.setAcceso(acceso(AccesoConstants.ACTIVO));
        usuario.setFechaInicioVigencia(hoy.minusDays(1));
        usuario.setFechaFinVigencia(null);
        assertTrue(service.esAdministrable(usuario));

        usuario.setAcceso(acceso(AccesoConstants.INACTIVO));
        assertFalse(service.esAdministrable(usuario));

        usuario.setAcceso(acceso(AccesoConstants.ACTIVO));
        usuario.setFechaFinVigencia(hoy.minusDays(1));
        assertFalse(service.esAdministrable(usuario));
    }

    private Acceso acceso(Long id) {
        Acceso acceso = new Acceso();
        acceso.setIdAcceso(id);
        return acceso;
    }
}
