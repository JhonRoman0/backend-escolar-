package com.example.Escolar.Controller;

import com.example.Escolar.Dto.EventoRequest;
import com.example.Escolar.Dto.EventoResponse;
import com.example.Escolar.Service.EventoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/eventos")
@RequiredArgsConstructor
public class EventoController {

    private final EventoService eventoService;

    @GetMapping
    public List<EventoResponse> getAll() {
        return eventoService.getAll();
    }

    @GetMapping("/{id}")
    public EventoResponse getById(@PathVariable Integer id) {
        return eventoService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventoResponse create(@Valid @RequestBody EventoRequest request) {
        return eventoService.create(request);
    }

    @PutMapping("/{id}")
    public EventoResponse update(@PathVariable Integer id, @Valid @RequestBody EventoRequest request) {
        return eventoService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        eventoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EventoResponse subirImagen(@PathVariable Integer id, @RequestPart("imagen") MultipartFile imagen) {
        return eventoService.subirImagen(id, imagen);
    }

    @DeleteMapping("/{id}/imagen")
    public ResponseEntity<Void> eliminarFoto(@PathVariable Integer id) {
        eventoService.eliminarFoto(id);
        return ResponseEntity.noContent().build();
    }
}