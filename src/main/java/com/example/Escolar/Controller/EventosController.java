package com.example.Escolar.Controller;

import com.example.Escolar.Service.AccesoContextoService;
import com.example.Escolar.Service.EventosService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Conexión de avisos en vivo: el navegador se entera de qué módulos cambiaron. */
@RestController
@RequiredArgsConstructor
public class EventosController {

    private final EventosService eventosService;
    private final AccesoContextoService accesoContextoService;

    @GetMapping(value = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter eventos(HttpServletResponse response) {
        // Nadie debe guardarla en caché ni retener los avisos (nginx, CloudFront).
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        return eventosService.conectar(accesoContextoService.idUsuarioAutenticado());
    }
}
