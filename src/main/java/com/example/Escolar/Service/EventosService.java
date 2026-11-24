package com.example.Escolar.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.LongFunction;

/**
 * Avisos en vivo (Server-Sent Events): cada navegador con sesión mantiene una
 * conexión abierta y, cuando alguien guarda algo, recibe qué módulos cambiaron
 * para recargar lo que tiene en pantalla. Solo se envían nombres de módulos,
 * nunca datos.
 *
 * Las conexiones viven en memoria: sirve con una sola instancia del backend.
 */
@Service
@Slf4j
public class EventosService {

    /** Tras este tiempo la conexión se cierra y el navegador se reconecta solo. */
    static final long DURACION_MS = 30 * 60 * 1000L;
    /** Pestañas abiertas por usuario; al pasar el tope se cierra la más antigua. */
    static final int MAXIMO_POR_USUARIO = 10;
    /** Cuánto espera el navegador para reintentar tras un corte breve. */
    static final long REINTENTO_MS = 2000L;

    private final Map<Integer, Deque<SseEmitter>> porUsuario = new ConcurrentHashMap<>();
    private final Set<String> pendientes = ConcurrentHashMap.newKeySet();
    /** Se reemplaza en los tests para simular conexiones que fallan. */
    LongFunction<SseEmitter> fabrica = SseEmitter::new;

    public SseEmitter conectar(Integer idUsuario) {
        SseEmitter emisor = fabrica.apply(DURACION_MS);
        Deque<SseEmitter> suyos = porUsuario.computeIfAbsent(idUsuario, k -> new ConcurrentLinkedDeque<>());
        emisor.onCompletion(() -> quitar(idUsuario, emisor));
        emisor.onTimeout(() -> quitar(idUsuario, emisor));
        emisor.onError(e -> quitar(idUsuario, emisor));
        suyos.addLast(emisor);
        while (suyos.size() > MAXIMO_POR_USUARIO) {
            SseEmitter antiguo = suyos.pollFirst();
            if (antiguo != null) {
                antiguo.complete();
            }
        }
        // Primer mensaje: cuánto esperar al reconectar y que la conexión quedó abierta.
        enviar(idUsuario, emisor, SseEmitter.event().reconnectTime(REINTENTO_MS).comment("conectado"));
        return emisor;
    }

    /** Anota un módulo cambiado; se envía agrupado con los demás del mismo segundo. */
    public void avisar(String modulo) {
        if (modulo != null && !modulo.isBlank()) {
            pendientes.add(modulo);
        }
    }

    /** Envía los cambios acumulados (agrupados) a todas las conexiones. */
    @Scheduled(fixedDelay = 1000)
    public void enviarPendientes() {
        if (pendientes.isEmpty()) {
            return;
        }
        Set<String> modulos = new TreeSet<>();
        for (String m : new ArrayList<>(pendientes)) {
            if (pendientes.remove(m)) {
                modulos.add(m);
            }
        }
        if (modulos.isEmpty()) {
            return;
        }
        String datos = "{\"modulos\":[" + String.join(",", modulos.stream().map(m -> "\"" + m + "\"").toList()) + "]}";
        aTodos(() -> SseEmitter.event().name("cambios").data(datos));
    }

    /**
     * Aviso solo a las pestañas de un usuario: cambió algo de su cuenta (inició
     * sesión en un dispositivo o quitó uno). Recargan sus dispositivos y revisan
     * su sesión; la de un dispositivo quitado se cierra.
     */
    public void avisarCuenta(Integer idUsuario) {
        Deque<SseEmitter> suyos = porUsuario.get(idUsuario);
        if (suyos == null) {
            return;
        }
        for (SseEmitter emisor : List.copyOf(suyos)) {
            enviar(idUsuario, emisor, SseEmitter.event().name("cuenta").data("{}"));
        }
    }

    /** Latido: evita que CloudFront (60 s) o nginx corten una conexión en silencio. */
    @Scheduled(fixedRate = 20_000, initialDelay = 20_000)
    public void latido() {
        aTodos(() -> SseEmitter.event().comment("latido"));
    }

    int conexiones() {
        return porUsuario.values().stream().mapToInt(Deque::size).sum();
    }

    int conexionesDe(Integer idUsuario) {
        Deque<SseEmitter> suyos = porUsuario.get(idUsuario);
        return suyos == null ? 0 : suyos.size();
    }

    private void aTodos(java.util.function.Supplier<SseEmitter.SseEventBuilder> evento) {
        for (Map.Entry<Integer, Deque<SseEmitter>> e : porUsuario.entrySet()) {
            for (SseEmitter emisor : List.copyOf(e.getValue())) {
                enviar(e.getKey(), emisor, evento.get());
            }
        }
    }

    private void enviar(Integer idUsuario, SseEmitter emisor, SseEmitter.SseEventBuilder evento) {
        try {
            emisor.send(evento);
        } catch (IOException | IllegalStateException e) {
            // La pestaña se cerró o la conexión murió: es normal, se descarta. No se
            // llama a completeWithError: el servidor ya sabe que el cliente se fue y
            // tratarlo como error terminaba en trazas en el log.
            log.debug("Conexión de avisos cerrada por el navegador: {}", e.getMessage());
            quitar(idUsuario, emisor);
        }
    }

    private void quitar(Integer idUsuario, SseEmitter emisor) {
        porUsuario.computeIfPresent(idUsuario, (k, suyos) -> {
            suyos.remove(emisor);
            return suyos.isEmpty() ? null : suyos;
        });
    }
}
