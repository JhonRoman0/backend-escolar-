package com.example.Escolar.Service;

import com.example.Escolar.Model.Turno;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AsistenciaServiceTest {

    private Turno turno() {
        Turno turno = new Turno();
        turno.setIdTurno(1);
        turno.setNombre("Mañana");
        turno.setHoraEntrada(LocalTime.of(8, 0));
        turno.setHoraEntradaLimite(LocalTime.of(8, 30));
        turno.setHoraFaltaLimite(LocalTime.of(9, 0));
        turno.setHoraSalida(LocalTime.of(12, 0));
        return turno;
    }

    @Test
    void ingresoAntesDeHoraEntradaEsPuntual() {
        assertEquals(AsistenciaService.ESTADO_PUNTUAL,
                AsistenciaService.calcularNombreEstado(LocalTime.of(7, 59, 59), turno()));
    }

    @Test
    void ingresoEnHoraEntradaExactaEsPuntual() {
        assertEquals(AsistenciaService.ESTADO_PUNTUAL,
                AsistenciaService.calcularNombreEstado(LocalTime.of(8, 0, 0), turno()));
    }

    @Test
    void ingresoHastaHoraEntradaLimiteEsPuntual() {
        assertEquals(AsistenciaService.ESTADO_PUNTUAL,
                AsistenciaService.calcularNombreEstado(LocalTime.of(8, 30, 0), turno()));
    }

    @Test
    void ingresoEntreLimiteYHoraFaltaEsTardanza() {
        assertEquals(AsistenciaService.ESTADO_TARDANZA,
                AsistenciaService.calcularNombreEstado(LocalTime.of(8, 30, 1), turno()));

        assertEquals(AsistenciaService.ESTADO_TARDANZA,
                AsistenciaService.calcularNombreEstado(LocalTime.of(9, 0, 0), turno()));
    }

    @Test
    void ingresoEntreHoraFaltaYHoraSalidaEsJustificada() {
        assertEquals(AsistenciaService.ESTADO_JUSTIFICADA,
                AsistenciaService.calcularNombreEstado(LocalTime.of(9, 0, 1), turno()));

        assertEquals(AsistenciaService.ESTADO_JUSTIFICADA,
                AsistenciaService.calcularNombreEstado(LocalTime.of(12, 0, 0), turno()));
    }

    @Test
    void ingresoDespuesDeHoraSalidaLanzaInasistencia() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AsistenciaService.calcularNombreEstado(LocalTime.of(12, 0, 1), turno()));
        assertEquals("El límite de ingreso ha expirado. El alumno queda como Inasistencia.", ex.getMessage());
    }

    @Test
    void marcadoAntesDeHoraEntradaSeRechaza() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AsistenciaService.validarVentanaIngreso(LocalTime.of(7, 59, 59), turno()));
        assertEquals("Registro denegado: aún no inicia la hora de entrada para el turno Mañana", ex.getMessage());
    }

    @Test
    void marcadoEnHoraEntradaExactaPasa() {
        assertDoesNotThrow(() -> AsistenciaService.validarVentanaIngreso(LocalTime.of(8, 0, 0), turno()));
    }
}