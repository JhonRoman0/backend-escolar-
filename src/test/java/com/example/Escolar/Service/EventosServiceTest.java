package com.example.Escolar.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EventosServiceTest {

    /** Emisor de prueba: guarda lo enviado y puede simular una pestaña cerrada. */
    static class Emisor extends SseEmitter {
        final List<Set<ResponseBodyEmitter.DataWithMediaType>> enviados = new ArrayList<>();
        boolean roto;
        boolean cerrado;
        Long duracion;

        Emisor(Long duracion) {
            super(duracion);
            this.duracion = duracion;
        }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (roto) {
                throw new IOException("pestaña cerrada");
            }
            enviados.add(builder.build());
        }

        @Override
        public synchronized void complete() {
            cerrado = true;
        }

        @Override
        public synchronized void completeWithError(Throwable ex) {
            cerrado = true;
        }

        String texto() {
            StringBuilder sb = new StringBuilder();
            enviados.forEach(e -> e.forEach(d -> sb.append(d.getData())));
            return sb.toString();
        }
    }

    EventosService servicio;
    List<Emisor> creados;

    @BeforeEach
    void preparar() {
        servicio = new EventosService();
        creados = new ArrayList<>();
        servicio.fabrica = duracion -> {
            Emisor e = new Emisor(duracion);
            creados.add(e);
            return e;
        };
    }

    @Test
    void elAvisoDeCuentaLlegaSoloALasPestanasDeEseUsuario() {
        servicio.conectar(7);
        servicio.conectar(7);
        servicio.conectar(8);
        int[] antes = creados.stream().mapToInt(e -> e.enviados.size()).toArray();

        servicio.avisarCuenta(7);
        servicio.avisarCuenta(99); // sin pestañas: no falla

        assertEquals(antes[0] + 1, creados.get(0).enviados.size());
        assertEquals(antes[1] + 1, creados.get(1).enviados.size());
        assertEquals(antes[2], creados.get(2).enviados.size());
        assertTrue(creados.get(0).enviados.get(antes[0]).stream()
                .anyMatch(d -> String.valueOf(d.getData()).contains("event:cuenta")));
    }

    @Test
    void laConexionDuraTreintaMinutosYAvisaCuandoReintentar() {
        servicio.conectar(7);
        assertEquals(EventosService.DURACION_MS, creados.get(0).duracion);
        assertTrue(creados.get(0).texto().contains("retry:2000"), creados.get(0).texto());
        assertEquals(1, servicio.conexiones());
    }

    @Test
    void losCambiosDelMismoSegundoSalenJuntos() {
        servicio.conectar(7);
        servicio.conectar(8);
        servicio.avisar("turnos");
        servicio.avisar("asistencias");
        servicio.avisar("turnos");

        servicio.enviarPendientes();

        for (Emisor e : creados) {
            assertTrue(e.texto().contains("{\"modulos\":[\"asistencias\",\"turnos\"]}"), e.texto());
            assertTrue(e.texto().contains("event:cambios"), e.texto());
        }
        // Lo enviado no se repite en la siguiente vuelta.
        int antes = creados.get(0).enviados.size();
        servicio.enviarPendientes();
        assertEquals(antes, creados.get(0).enviados.size());
    }

    @Test
    void unaPestanaCerradaSeDescartaAlEnviarle() {
        servicio.conectar(7);
        servicio.conectar(7);
        creados.get(0).roto = true;

        servicio.latido();

        // Se quita de la lista; no se procesa como error (el servidor ya sabe que se fue).
        assertEquals(1, servicio.conexionesDe(7));
        assertFalse(creados.get(1).roto);
    }

    @Test
    void masDeDiezPestanasCierranLaMasAntigua() {
        for (int i = 0; i < EventosService.MAXIMO_POR_USUARIO + 2; i++) {
            servicio.conectar(7);
        }
        assertEquals(EventosService.MAXIMO_POR_USUARIO, servicio.conexionesDe(7));
        assertTrue(creados.get(0).cerrado);
        assertTrue(creados.get(1).cerrado);
        assertFalse(creados.get(2).cerrado);
    }

    @Test
    void sinCambiosNoSeEnviaNada() {
        servicio.conectar(7);
        int antes = creados.get(0).enviados.size();
        servicio.enviarPendientes();
        assertEquals(antes, creados.get(0).enviados.size());
    }
}
