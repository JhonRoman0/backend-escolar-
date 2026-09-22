package com.example.Escolar.Controller;

import com.example.Escolar.Dto.GaleriaRequest;
import com.example.Escolar.Dto.GaleriaResponse;
import com.example.Escolar.Service.GaleriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/galerias")
@RequiredArgsConstructor
public class GaleriaController {

    private final GaleriaService galeriaService;

    @GetMapping
    public List<GaleriaResponse> getAll() {
        return galeriaService.getAll();
    }

    @GetMapping("/{id}")
    public GaleriaResponse getById(@PathVariable Integer id) {
        return galeriaService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GaleriaResponse create(@Valid @RequestBody GaleriaRequest request) {
        return galeriaService.create(request);
    }

    @PutMapping("/{id}")
    public GaleriaResponse update(@PathVariable Integer id, @Valid @RequestBody GaleriaRequest request) {
        return galeriaService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        galeriaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GaleriaResponse agregarFotos(@PathVariable Integer id, @RequestPart("fotos") List<MultipartFile> fotos) {
        return galeriaService.agregarFotos(id, fotos);
    }

    @DeleteMapping("/fotos/{idDetalle}")
    public ResponseEntity<Void> eliminarDetalle(@PathVariable Integer idDetalle) {
        galeriaService.eliminarDetalle(idDetalle);
        return ResponseEntity.noContent().build();
    }
}