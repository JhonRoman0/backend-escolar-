package com.example.Escolar.Controller;

import com.example.Escolar.Dto.ColegioRequest;
import com.example.Escolar.Dto.ColegioResponse;
import com.example.Escolar.Service.ColegioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/colegios")
@RequiredArgsConstructor
public class ColegioController {

    private final ColegioService colegioService;

    @GetMapping
    public List<ColegioResponse> getAll() {
        return colegioService.getAll();
    }

    @GetMapping("/actual")
    public ColegioResponse getActual() {
        return colegioService.getActual();
    }

    @GetMapping("/{id}")
    public ColegioResponse getById(@PathVariable Integer id) {
        return colegioService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ColegioResponse create(@Valid @RequestBody ColegioRequest request) {
        return colegioService.create(request);
    }

    @PutMapping("/{id}")
    public ColegioResponse update(@PathVariable Integer id, @Valid @RequestBody ColegioRequest request) {
        return colegioService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        colegioService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ColegioResponse subirFoto(@PathVariable Integer id, @RequestPart("imagen") MultipartFile imagen) {
        return colegioService.subirFoto(id, imagen);
    }

    @DeleteMapping("/{id}/foto")
    public ResponseEntity<Void> eliminarFoto(@PathVariable Integer id) {
        colegioService.eliminarFoto(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/portada", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ColegioResponse subirPortada(@PathVariable Integer id, @RequestPart("imagen") MultipartFile imagen) {
        return colegioService.subirPortada(id, imagen);
    }

    @DeleteMapping("/{id}/portada")
    public ResponseEntity<Void> eliminarPortada(@PathVariable Integer id) {
        colegioService.eliminarPortada(id);
        return ResponseEntity.noContent().build();
    }
}