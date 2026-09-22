package com.example.Escolar.Controller;

import com.example.Escolar.Dto.AjusteResponse;
import com.example.Escolar.Dto.ColegioResponse;
import com.example.Escolar.Dto.ContactoMensajeRequest;
import com.example.Escolar.Dto.ContactoMensajeResponse;
import com.example.Escolar.Dto.EventoResponse;
import com.example.Escolar.Dto.GaleriaResponse;
import com.example.Escolar.Dto.PublicacionResponse;
import com.example.Escolar.Service.AjusteService;
import com.example.Escolar.Service.ColegioService;
import com.example.Escolar.Service.ContactoMensajeService;
import com.example.Escolar.Service.EventoService;
import com.example.Escolar.Service.GaleriaService;
import com.example.Escolar.Service.PublicacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/portal")
@RequiredArgsConstructor
public class PortalPublicoController {

    private final PublicacionService publicacionService;
    private final EventoService eventoService;
    private final GaleriaService galeriaService;
    private final AjusteService ajusteService;
    private final ContactoMensajeService contactoMensajeService;
    private final ColegioService colegioService;

    @GetMapping("/publicaciones")
    public List<PublicacionResponse> getPublicaciones() {
        return publicacionService.getPublicas();
    }

    @GetMapping("/eventos")
    public List<EventoResponse> getEventos() {
        return eventoService.getPublicos();
    }

    @GetMapping("/galerias")
    public List<GaleriaResponse> getGalerias() {
        return galeriaService.getAll();
    }

    @GetMapping("/ajustes")
    public List<AjusteResponse> getAjustes() {
        return ajusteService.getAll();
    }

    @GetMapping("/colegio")
    public ColegioResponse getColegio() {
        return colegioService.getActual();
    }

    @PostMapping("/contacto")
    @ResponseStatus(HttpStatus.CREATED)
    public ContactoMensajeResponse enviarContacto(@Valid @RequestBody ContactoMensajeRequest request) {
        return contactoMensajeService.crearPublico(request);
    }
}